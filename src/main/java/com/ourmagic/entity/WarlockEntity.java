package com.ourmagic.entity;

import com.mojang.authlib.GameProfile;
import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.magic.spell.runtime.MagicAllies;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.WandData;
import com.ourmagic.wand.WandTemplates;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.EnumSet;
import java.util.UUID;

public class WarlockEntity extends Monster {
    private static final int MAX_MANA = 50;
    private static final int MANA_REGEN_INTERVAL = 20;
    private static final List<String> WARLOCK_SPELLS = List.of(
            "missile@target",
            "arrow@target",
            "fireball@target",
            "blind@target",
            "bind@target",
            "hex@target",
            "stun@target",
            "heal@target",
            "regenerate@target",
            "shield@self"
    );
    private static final GameProfile CAST_PROFILE = new GameProfile(UUID.fromString("9c9db3d0-6f4c-405a-a291-55c57379ed4c"), "[OurMagicWarlock]");
    private int mana = MAX_MANA;

    public WarlockEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 32.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new WarlockDefensiveGoal(this));
        goalSelector.addGoal(3, new WarlockHealAllyGoal(this));
        goalSelector.addGoal(4, new WarlockCastGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, WarlockEntity::isWarlockEnemy));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && tickCount % MANA_REGEN_INTERVAL == 0 && mana < MAX_MANA) {
            mana++;
        }
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, createWarlockWand(random));
        setDropChance(EquipmentSlot.MAINHAND, 0.12F);
    }

    private static ItemStack createWarlockWand(RandomSource random) {
        String key = WARLOCK_SPELLS.get(random.nextInt(WARLOCK_SPELLS.size()));
        Spell spell = SpellRegistry.get(key);
        ItemStack wand = new ItemStack(ModItems.WAND.get());
        if (spell == null && !"missile@target".equals(key)) {
            key = "missile@target";
            spell = SpellRegistry.get(key);
        }
        if (spell == null) {
            return wand;
        }
        int manaCost = Math.max(1, Math.round(spell.manaCost() * 0.85F));
        int cooldown = Math.max(20, spell.cooldownTicks());
        new WandData(WandTemplates.DEFAULT_TEMPLATE, "Warlock Wand", 0.95F + random.nextFloat() * 0.25F,
                List.of(new WandData.WandSpellData(key, manaCost, cooldown)), 0, 0).save(wand);
        return wand;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData spawnData = super.finalizeSpawn(level, difficulty, reason, data, tag);
        if (getMainHandItem().isEmpty()) {
            populateDefaultEquipmentSlots(getRandom(), difficulty);
        }
        return spawnData;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("OurMagicMana", mana);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        mana = tag.contains("OurMagicMana") ? Mth.clamp(tag.getInt("OurMagicMana"), 0, MAX_MANA) : MAX_MANA;
    }

    boolean tryCastAt(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel) || target == null || !target.isAlive()) {
            return false;
        }

        ItemStack wand = getMainHandItem();
        if (!wand.is(ModItems.WAND.get()) && !wand.is(ModItems.ADMIN_WAND.get())) {
            return false;
        }

        if (!wand.getOrCreateTag().contains(WandData.TAG_TEMPLATE)) {
            wand = createWarlockWand(getRandom());
            setItemSlot(EquipmentSlot.MAINHAND, wand);
        }
        WandData data = WandData.read(wand);
        Spell spell = SpellRegistry.get(data.activeSpell());
        if (spell == null || data.cooldownUntil() > level().getGameTime() || mana < data.activeManaCost()) {
            return false;
        }

        ServerPlayer caster = FakePlayerFactory.get(serverLevel, CAST_PROFILE);
        aimFakeCaster(caster, target);
        boolean cast = spell.cast(serverLevel, caster, wand, data, 1.0F);
        if (cast) {
            mana -= data.activeManaCost();
            data.setCooldownUntil(level().getGameTime() + data.activeCooldownTicks());
            data.save(wand);
            setItemSlot(EquipmentSlot.MAINHAND, wand);
            serverLevel.playSound(null, blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 0.8F, 0.75F + getRandom().nextFloat() * 0.35F);
        }
        return cast;
    }

    private boolean hasSupportSpell() {
        return isSupportSpell(activeSpellKey());
    }

    private boolean hasShieldSpell() {
        return SpellRegistry.payloadParts(activeSpellKey()).contains("shield");
    }

    private String activeSpellKey() {
        ItemStack wand = getMainHandItem();
        if (!wand.is(ModItems.WAND.get()) && !wand.is(ModItems.ADMIN_WAND.get())) {
            return "";
        }
        if (!wand.getOrCreateTag().contains(WandData.TAG_TEMPLATE)) {
            wand = createWarlockWand(getRandom());
            setItemSlot(EquipmentSlot.MAINHAND, wand);
        }
        return WandData.read(wand).activeSpell();
    }

    private static boolean isSupportSpell(String spellKey) {
        List<String> payloads = SpellRegistry.payloadParts(spellKey);
        return payloads.contains("heal") || payloads.contains("regenerate") || payloads.contains("life_ward");
    }

    private static boolean isWarlockAlly(WarlockEntity warlock, LivingEntity entity) {
        return entity != warlock
                && entity.isAlive()
                && entity.getHealth() < entity.getMaxHealth()
                && entity instanceof Monster
                && !(entity instanceof Player)
                && !MagicAllies.isPlayerSummon(entity);
    }

    private static boolean isWarlockEnemy(LivingEntity entity) {
        return entity instanceof Player || MagicAllies.isPlayerSummon(entity);
    }

    private void aimFakeCaster(ServerPlayer caster, LivingEntity target) {
        Vec3 eye = getEyePosition();
        Vec3 targetEye = target.getEyePosition();
        Vec3 direction = targetEye.subtract(eye);
        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(direction.y, horizontal) * Mth.RAD_TO_DEG);

        caster.moveTo(getX(), getY(), getZ(), yaw, pitch);
        caster.setYRot(yaw);
        caster.setXRot(pitch);
        caster.yHeadRot = yaw;
        caster.yBodyRot = yaw;
        lookAt(target, 30.0F, 30.0F);
    }

    private static final class WarlockCastGoal extends Goal {
        private final WarlockEntity warlock;
        private int attackTime;

        private WarlockCastGoal(WarlockEntity warlock) {
            this.warlock = warlock;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !warlock.hasSupportSpell() && warlock.getTarget() != null && warlock.getTarget().isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void tick() {
            LivingEntity target = warlock.getTarget();
            if (target == null) {
                return;
            }

            double distance = warlock.distanceToSqr(target);
            warlock.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (distance > 36.0D) {
                warlock.getNavigation().moveTo(target, 0.9D);
            } else if (distance < 12.0D) {
                warlock.getNavigation().moveTo(warlock.getX() - (target.getX() - warlock.getX()), warlock.getY(), warlock.getZ() - (target.getZ() - warlock.getZ()), 0.75D);
            } else {
                warlock.getNavigation().stop();
            }

            if (--attackTime <= 0 && distance < 625.0D && warlock.hasLineOfSight(target)) {
                if (warlock.tryCastAt(target)) {
                    attackTime = 10;
                } else {
                    attackTime = 20;
                }
            }
        }
    }

    private static final class WarlockDefensiveGoal extends Goal {
        private final WarlockEntity warlock;
        private Projectile threat;
        private int dodgeTicks;
        private int shieldTime;

        private WarlockDefensiveGoal(WarlockEntity warlock) {
            this.warlock = warlock;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            threat = findIncomingProjectile();
            return threat != null || warlock.hasShieldSpell() && warlock.getHealth() < warlock.getMaxHealth() * 0.55F;
        }

        @Override
        public boolean canContinueToUse() {
            return dodgeTicks > 0 && threat != null && threat.isAlive();
        }

        @Override
        public void start() {
            dodgeTicks = threat == null ? 0 : 12;
        }

        @Override
        public void stop() {
            threat = null;
            dodgeTicks = 0;
        }

        @Override
        public void tick() {
            if (warlock.hasShieldSpell() && --shieldTime <= 0 && (threat != null || warlock.getHealth() < warlock.getMaxHealth() * 0.55F)) {
                if (warlock.tryCastAt(warlock)) {
                    shieldTime = 80;
                } else {
                    shieldTime = 20;
                }
            }

            if (threat == null || !threat.isAlive()) {
                threat = findIncomingProjectile();
            }
            if (threat == null) {
                return;
            }

            Vec3 projectileVelocity = threat.getDeltaMovement();
            Vec3 toWarlock = warlock.position().subtract(threat.position());
            Vec3 dodge = new Vec3(-projectileVelocity.z, 0.0D, projectileVelocity.x);
            if (dodge.lengthSqr() < 0.001D) {
                dodge = new Vec3(-toWarlock.z, 0.0D, toWarlock.x);
            }
            dodge = dodge.normalize();
            if (warlock.getRandom().nextBoolean()) {
                dodge = dodge.scale(-1.0D);
            }

            Vec3 destination = warlock.position().add(dodge.scale(4.0D));
            warlock.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.05D);
            warlock.getLookControl().setLookAt(threat, 30.0F, 30.0F);
            dodgeTicks--;
        }

        @Nullable
        private Projectile findIncomingProjectile() {
            return warlock.level().getEntitiesOfClass(Projectile.class, warlock.getBoundingBox().inflate(8.0D),
                            projectile -> projectile.isAlive() && projectile.getOwner() != warlock && isIncoming(projectile))
                    .stream()
                    .min(java.util.Comparator.comparingDouble(warlock::distanceToSqr))
                    .orElse(null);
        }

        private boolean isIncoming(Projectile projectile) {
            Vec3 velocity = projectile.getDeltaMovement();
            if (velocity.lengthSqr() < 0.0025D) {
                return false;
            }
            Vec3 toWarlock = warlock.getEyePosition().subtract(projectile.position());
            if (toWarlock.lengthSqr() < 0.01D) {
                return true;
            }
            return velocity.normalize().dot(toWarlock.normalize()) > 0.65D;
        }
    }

    private static final class WarlockHealAllyGoal extends Goal {
        private final WarlockEntity warlock;
        private LivingEntity ally;
        private int supportTime;

        private WarlockHealAllyGoal(WarlockEntity warlock) {
            this.warlock = warlock;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!warlock.hasSupportSpell()) {
                return false;
            }
            ally = findInjuredAlly();
            return ally != null;
        }

        @Override
        public boolean canContinueToUse() {
            return ally != null && isWarlockAlly(warlock, ally) && warlock.hasSupportSpell();
        }

        @Override
        public void stop() {
            ally = null;
            warlock.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (ally == null) {
                return;
            }

            double distance = warlock.distanceToSqr(ally);
            warlock.getLookControl().setLookAt(ally, 30.0F, 30.0F);
            if (distance > 64.0D) {
                warlock.getNavigation().moveTo(ally, 0.9D);
            } else {
                warlock.getNavigation().stop();
            }

            if (--supportTime <= 0 && distance < 625.0D && warlock.hasLineOfSight(ally)) {
                if (warlock.tryCastAt(ally)) {
                    supportTime = 30;
                } else {
                    supportTime = 20;
                }
            }
        }

        @Nullable
        private LivingEntity findInjuredAlly() {
            return warlock.level().getEntitiesOfClass(LivingEntity.class, warlock.getBoundingBox().inflate(18.0D),
                            entity -> isWarlockAlly(warlock, entity))
                    .stream()
                    .min(java.util.Comparator.comparingDouble(warlock::distanceToSqr))
                    .orElse(null);
        }
    }
}
