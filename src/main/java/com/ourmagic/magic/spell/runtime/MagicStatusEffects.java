package com.ourmagic.magic.spell.runtime;

import com.ourmagic.OurMagic;
import com.ourmagic.magic.spell.shapes.SpellTarget;
<<<<<<< HEAD
<<<<<<< HEAD
=======
import com.ourmagic.mana.PlayerMana;
>>>>>>> ```markdown
=======
>>>>>>> 66d784116cfd50799180a63552b78a7b327bba34
import com.ourmagic.network.ModNetwork;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
<<<<<<< HEAD
<<<<<<< HEAD
import net.minecraft.world.effect.MobEffect;
=======
>>>>>>> ```markdown
=======
import net.minecraft.world.effect.MobEffect;
>>>>>>> 66d784116cfd50799180a63552b78a7b327bba34
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class MagicStatusEffects {
    private static final Map<UUID, BoundState> BOUND = new HashMap<>();
    private static final Map<UUID, StunState> STUNNED = new HashMap<>();
    private static final Map<UUID, WarpState> WARPED = new HashMap<>();
    private static final Map<UUID, TimedState> SILENCED = new HashMap<>();
    private static final Map<UUID, WardState> WARDED = new HashMap<>();
    private static final Map<UUID, CharmState> CHARMED = new HashMap<>();
    private static final Map<UUID, TimedState> REFLECTING = new HashMap<>();
    private static final Map<UUID, TimedState> ANCHORED = new HashMap<>();
    private static final Map<UUID, HexState> HEXED = new HashMap<>();
    private static final Map<UUID, ManaShieldState> MANA_SHIELDED = new HashMap<>();
    private static final Map<UUID, LifeWardState> LIFE_WARDED = new HashMap<>();
    private static final Map<UUID, TimedState> FALLGUARDED = new HashMap<>();
    private static final Map<UUID, TimedState> BLASTGUARDED = new HashMap<>();

    private MagicStatusEffects() {
    }

    public static void bind(LivingEntity entity, int ticks) {
        BOUND.put(entity.getUUID(), new BoundState(ticks, entity.position()));
        entity.setDeltaMovement(Vec3.ZERO);
    }

    public static void stun(LivingEntity entity, int ticks) {
        stun(entity);
    }

    public static void stun(LivingEntity entity) {
        StunState existing = STUNNED.get(entity.getUUID());
        STUNNED.put(entity.getUUID(), new StunState(existing == null ? entity.position() : existing.anchor()));
        entity.setDeltaMovement(Vec3.ZERO);
        entity.stopRiding();
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
        }
        if (entity instanceof ServerPlayer player) {
            player.setPose(Pose.SWIMMING);
            ModNetwork.syncStunState(player, true);
        }
    }

    public static void warp(ServerPlayer player, int ticks) {
        WarpState existing = WARPED.get(player.getUUID());
        WARPED.put(player.getUUID(), new WarpState(Math.max(ticks, existing == null ? 0 : existing.ticks()), existing == null ? player.gameMode.getGameModeForPlayer() : existing.previousMode()));
        player.setGameMode(GameType.SPECTATOR);
    }

    public static void silence(LivingEntity entity, int ticks) {
        SILENCED.put(entity.getUUID(), new TimedState(ticks));
    }

    public static boolean isSilenced(Entity entity) {
        return SILENCED.containsKey(entity.getUUID());
    }

    public static void ward(LivingEntity entity, int ticks, float strength) {
        WardState existing = WARDED.get(entity.getUUID());
        WARDED.put(entity.getUUID(), new WardState(Math.max(ticks, existing == null ? 0 : existing.ticks()), Math.max(strength, existing == null ? 0.0F : existing.strength())));
    }

    public static void charm(Mob mob, LivingEntity caster, int ticks) {
        CHARMED.put(mob.getUUID(), new CharmState(ticks, caster.getUUID()));
        mob.setTarget(null);
    }

    public static void reflect(LivingEntity entity, int ticks) {
        REFLECTING.put(entity.getUUID(), new TimedState(ticks));
    }

    public static boolean tryReflectSpell(SpellContext context, SpellTarget target) {
        if (!context.spell().isPhysical() || target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        TimedState reflect = REFLECTING.remove(living.getUUID());
        if (reflect == null) {
            return false;
        }

        Entity attacker = context.player();
        attacker.hurt(MagicDamageSources.playerMagic(context.level(), context.player()), 4.0F);
        if (living.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLASH, living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(), 35, 0.55D, 0.55D, 0.55D, 0.06D);
        }
        return true;
    }

    public static void anchor(LivingEntity entity, int ticks) {
        ANCHORED.put(entity.getUUID(), new TimedState(ticks));
        entity.setDeltaMovement(Vec3.ZERO);
    }

    public static void hex(LivingEntity entity, int ticks, float damageMultiplier) {
        HEXED.put(entity.getUUID(), new HexState(ticks, Math.max(1.0F, damageMultiplier)));
    }

    public static void manaShield(ServerPlayer player, int ticks, float absorptionRatio) {
        ManaShieldState existing = MANA_SHIELDED.get(player.getUUID());
        MANA_SHIELDED.put(player.getUUID(), new ManaShieldState(
                Math.max(ticks, existing == null ? 0 : existing.ticks()),
                Math.max(absorptionRatio, existing == null ? 0.0F : existing.absorptionRatio())));
    }

    public static void lifeWard(LivingEntity entity, int ticks, float power) {
        LifeWardState existing = LIFE_WARDED.get(entity.getUUID());
        LIFE_WARDED.put(entity.getUUID(), new LifeWardState(
                Math.max(ticks, existing == null ? 0 : existing.ticks()),
                Math.max(power, existing == null ? 0.0F : existing.power())));
    }

    public static void fallguard(LivingEntity entity, int ticks) {
        TimedState existing = FALLGUARDED.get(entity.getUUID());
        FALLGUARDED.put(entity.getUUID(), new TimedState(Math.max(ticks, existing == null ? 0 : existing.ticks())));
    }

    public static void blastguard(LivingEntity entity, int ticks) {
        TimedState existing = BLASTGUARDED.get(entity.getUUID());
        BLASTGUARDED.put(entity.getUUID(), new TimedState(Math.max(ticks, existing == null ? 0 : existing.ticks())));
    }

    public static void nullify(LivingEntity entity) {
        BOUND.remove(entity.getUUID());
        SILENCED.remove(entity.getUUID());
        WARDED.remove(entity.getUUID());
        CHARMED.remove(entity.getUUID());
        REFLECTING.remove(entity.getUUID());
        ANCHORED.remove(entity.getUUID());
        HEXED.remove(entity.getUUID());
        MANA_SHIELDED.remove(entity.getUUID());
        LIFE_WARDED.remove(entity.getUUID());
        FALLGUARDED.remove(entity.getUUID());
        BLASTGUARDED.remove(entity.getUUID());
        Scrying.clearMarksOn(entity);
        wake(entity);
        WarpState warp = WARPED.remove(entity.getUUID());
        if (warp != null && entity instanceof ServerPlayer player) {
            player.setGameMode(warp.previousMode());
        }

        removeNegativeConditions(entity);
        entity.removeEffect(MobEffects.BLINDNESS);
        entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        entity.removeEffect(MobEffects.LEVITATION);
        entity.removeEffect(MobEffects.WATER_BREATHING);
        entity.removeEffect(MobEffects.REGENERATION);
        entity.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        entity.removeEffect(MobEffects.ABSORPTION);
        entity.removeEffect(MobEffects.FIRE_RESISTANCE);
        entity.removeEffect(MobEffects.MOVEMENT_SPEED);
        entity.removeEffect(MobEffects.JUMP);
        entity.removeEffect(MobEffects.SLOW_FALLING);
        entity.removeEffect(MobEffects.GLOWING);
        entity.removeEffect(MobEffects.WEAKNESS);
        entity.removeEffect(MobEffects.DIG_SLOWDOWN);
        entity.removeEffect(MobEffects.CONFUSION);
    }

    public static void cleanse(LivingEntity entity) {
        nullify(entity);
        removeNegativeConditions(entity);
    }

    private static void removeNegativeConditions(LivingEntity entity) {
        List<MobEffect> effects = new ArrayList<>();
        for (MobEffectInstance instance : entity.getActiveEffects()) {
            MobEffect effect = instance.getEffect();
            if (!effect.isBeneficial()) {
                effects.add(effect);
            }
        }
        effects.forEach(entity::removeEffect);
        entity.clearFire();
        entity.setAirSupply(entity.getMaxAirSupply());
        entity.setTicksFrozen(0);
    }

    public static boolean isBound(Entity entity) {
        return BOUND.containsKey(entity.getUUID());
    }

    public static boolean isStunned(Entity entity) {
        return STUNNED.containsKey(entity.getUUID());
    }

    public static void wake(LivingEntity entity) {
        if (STUNNED.remove(entity.getUUID()) != null && entity instanceof ServerPlayer player) {
            player.setPose(Pose.STANDING);
            ModNetwork.syncStunState(player, false);
        }
    }

    @SubscribeEvent
    public static void livingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        BoundState bound = BOUND.get(entity.getUUID());
        TimedState anchor = ANCHORED.get(entity.getUUID());
        if (bound != null || anchor != null) {
            entity.setDeltaMovement(Vec3.ZERO);
            if (bound != null) {
                entity.teleportTo(bound.anchor().x, bound.anchor().y, bound.anchor().z);
            }
            if (entity.level() instanceof ServerLevel serverLevel && entity.tickCount % 10 == 0) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 8, 0.35D, 0.35D, 0.35D, 0.01D);
            }
        }

        if (entity instanceof Mob mob) {
            CharmState charm = CHARMED.get(entity.getUUID());
            if (charm != null) {
                mob.setTarget(null);
                if (entity.level() instanceof ServerLevel serverLevel && entity.tickCount % 12 == 0) {
                    serverLevel.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + entity.getBbHeight() * 0.75D, entity.getZ(), 2, 0.25D, 0.2D, 0.25D, 0.01D);
                }
            }
        }

        StunState stun = STUNNED.get(entity.getUUID());
        if (stun != null && entity.level() instanceof ServerLevel serverLevel) {
            if (!entity.isAlive()) {
                STUNNED.remove(entity.getUUID());
                if (entity instanceof ServerPlayer player) {
                    ModNetwork.syncStunState(player, false);
                }
                return;
            }
            entity.setDeltaMovement(Vec3.ZERO);
            entity.teleportTo(stun.anchor().x, stun.anchor().y, stun.anchor().z);
            if (entity instanceof Mob mob) {
                mob.setTarget(null);
            }
            if (entity instanceof ServerPlayer player) {
                player.setPose(Pose.SWIMMING);
            }
            if (entity.tickCount % 10 == 0) {
                serverLevel.sendParticles(ParticleTypes.WITCH, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 6, 0.25D, 0.2D, 0.25D, 0.01D);
            }
        }

        if (FALLGUARDED.containsKey(entity.getUUID())) {
            entity.fallDistance = 0.0F;
            if (entity.level() instanceof ServerLevel serverLevel && entity.tickCount % 12 == 0) {
                serverLevel.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.15D, entity.getZ(), 4, 0.35D, 0.05D, 0.35D, 0.01D);
            }
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        WarpState warp = WARPED.get(player.getUUID());
        if (warp != null) {
            if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                WARPED.remove(player.getUUID());
                return;
            }
            if (warp.ticks() <= 1) {
                WARPED.remove(player.getUUID());
                player.setGameMode(warp.previousMode());
            } else {
                WARPED.put(player.getUUID(), new WarpState(warp.ticks() - 1, warp.previousMode()));
            }
        }

        StunState stun = STUNNED.get(player.getUUID());
        if (stun != null) {
            player.setDeltaMovement(Vec3.ZERO);
            player.teleportTo(stun.anchor().x, stun.anchor().y, stun.anchor().z);
            player.setPose(Pose.SWIMMING);
        }

        if (SILENCED.containsKey(player.getUUID()) && player.level() instanceof ServerLevel serverLevel && player.tickCount % 10 == 0) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(), 5, 0.25D, 0.25D, 0.25D, 0.01D);
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        tickBound();
        tickSimple(SILENCED);
        tickSimple(WARDED);
        tickSimple(CHARMED);
        tickSimple(REFLECTING);
        tickSimple(ANCHORED);
        tickSimple(HEXED);
        tickSimple(MANA_SHIELDED);
        tickSimple(LIFE_WARDED);
        tickSimple(FALLGUARDED);
        tickSimple(BLASTGUARDED);
    }

    @SubscribeEvent
    public static void teleport(EntityTeleportEvent event) {
        if (isBound(event.getEntity()) || isStunned(event.getEntity()) || ANCHORED.containsKey(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void livingAttack(LivingAttackEvent event) {
<<<<<<< HEAD
<<<<<<< HEAD
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && isStunned(attacker)) {
            event.setCanceled(true);
=======
        if (event.getSource().is(DamageTypes.FALL) && FALLGUARDED.containsKey(event.getEntity().getUUID())) {
            event.getEntity().fallDistance = 0.0F;
            event.setCanceled(true);
            if (event.getEntity().level() instanceof ServerLevel serverLevel) {
                LivingEntity entity = event.getEntity();
                serverLevel.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + 0.15D, entity.getZ(), 18, 0.45D, 0.08D, 0.45D, 0.02D);
            }
            return;
        }

        if (isExplosionDamage(event.getSource()) && BLASTGUARDED.containsKey(event.getEntity().getUUID())) {
            event.setCanceled(true);
            if (event.getEntity().level() instanceof ServerLevel serverLevel) {
                LivingEntity entity = event.getEntity();
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 32, 0.55D, 0.55D, 0.55D, 0.035D);
            }
>>>>>>> ```markdown
=======
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && isStunned(attacker)) {
            event.setCanceled(true);
>>>>>>> 66d784116cfd50799180a63552b78a7b327bba34
            return;
        }

        if (event.getSource().is(DamageTypes.MAGIC)) {
            return;
        }

        TimedState reflect = REFLECTING.remove(event.getEntity().getUUID());
        if (reflect != null) {
            event.setCanceled(true);
            if (attacker instanceof LivingEntity livingAttacker) {
                livingAttacker.hurt(MagicDamageSources.reflectedMagic(attacker), 4.0F);
            }
            if (event.getEntity().level() instanceof ServerLevel serverLevel) {
                LivingEntity entity = event.getEntity();
                serverLevel.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 35, 0.55D, 0.55D, 0.55D, 0.06D);
            }
            return;
        }

        WardState ward = WARDED.remove(event.getEntity().getUUID());
        if (ward != null) {
            event.setCanceled(true);
            if (event.getEntity().level() instanceof ServerLevel serverLevel) {
                LivingEntity entity = event.getEntity();
                serverLevel.sendParticles(ParticleTypes.END_ROD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), Math.max(18, Math.round(18 * ward.strength())), 0.45D, 0.45D, 0.45D, 0.05D);
                serverLevel.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            return;
        }

        if (attacker != null) {
            wake(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void livingHurt(LivingHurtEvent event) {
        ManaShieldState manaShield = MANA_SHIELDED.get(event.getEntity().getUUID());
        if (manaShield != null && event.getEntity() instanceof ServerPlayer player) {
            PlayerMana mana = PlayerMana.get(player);
            int manaAvailable = mana.mana();
            if (manaAvailable > 0) {
                float absorbed = Math.min(event.getAmount() * manaShield.absorptionRatio(), manaAvailable);
                if (absorbed > 0.0F && mana.spend(Math.max(1, Math.round(absorbed)))) {
                    event.setAmount(Math.max(0.0F, event.getAmount() - absorbed));
                    ModNetwork.syncMana(player, mana);
                    if (player.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(), 18, 0.45D, 0.45D, 0.45D, 0.04D);
                    }
                }
            }
        }

        LifeWardState lifeWard = LIFE_WARDED.get(event.getEntity().getUUID());
        if (lifeWard != null && event.getAmount() >= event.getEntity().getHealth()) {
            LIFE_WARDED.remove(event.getEntity().getUUID());
            event.setAmount(0.0F);
            LivingEntity entity = event.getEntity();
            entity.setHealth(Math.min(entity.getMaxHealth(), Math.max(4.0F, 4.0F * lifeWard.power())));
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Math.round(20 * 6 * lifeWard.power()), 1));
            entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, Math.round(20 * 10 * lifeWard.power()), 1));
            if (entity.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 45, 0.55D, 0.55D, 0.55D, 0.08D);
                serverLevel.sendParticles(ParticleTypes.FLASH, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            return;
        }

        HexState hex = HEXED.remove(event.getEntity().getUUID());
        if (hex == null) {
            return;
        }

        event.setAmount(event.getAmount() * hex.damageMultiplier());
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            LivingEntity entity = event.getEntity();
            serverLevel.sendParticles(ParticleTypes.WITCH, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 28, 0.45D, 0.45D, 0.45D, 0.04D);
        }
    }

    @SubscribeEvent
    public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isStunned(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (event.getTarget() instanceof LivingEntity target && isStunned(target)) {
            wake(target);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void attackEntity(AttackEntityEvent event) {
        if (isStunned(event.getEntity())) {
            event.setCanceled(true);
            return;
        }
        if (event.getTarget() instanceof LivingEntity target && isStunned(target)) {
            wake(target);
        }
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isStunned(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void rightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isStunned(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isStunned(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            wake(player);
            WarpState warp = WARPED.remove(player.getUUID());
            if (warp != null) {
                player.setGameMode(warp.previousMode());
            }
            SILENCED.remove(player.getUUID());
            WARDED.remove(player.getUUID());
            CHARMED.remove(player.getUUID());
            REFLECTING.remove(player.getUUID());
            ANCHORED.remove(player.getUUID());
            HEXED.remove(player.getUUID());
            MANA_SHIELDED.remove(player.getUUID());
            LIFE_WARDED.remove(player.getUUID());
            FALLGUARDED.remove(player.getUUID());
            BLASTGUARDED.remove(player.getUUID());
        }
    }

    private static boolean isExplosionDamage(net.minecraft.world.damagesource.DamageSource source) {
        return source.is(DamageTypes.EXPLOSION)
                || source.is(DamageTypes.PLAYER_EXPLOSION)
                || source.is(DamageTypes.BAD_RESPAWN_POINT);
    }

    private static void tickBound() {
        Iterator<Map.Entry<UUID, BoundState>> iterator = BOUND.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BoundState> entry = iterator.next();
            BoundState state = entry.getValue();
            if (state.ticks() <= 1) {
                iterator.remove();
            } else {
                entry.setValue(new BoundState(state.ticks() - 1, state.anchor()));
            }
        }
    }

    private static <T extends Timed<T>> void tickSimple(Map<UUID, T> states) {
        Iterator<Map.Entry<UUID, T>> iterator = states.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, T> entry = iterator.next();
            T state = entry.getValue();
            if (state.ticks() <= 1) {
                iterator.remove();
            } else {
                entry.setValue(state.withTicks(state.ticks() - 1));
            }
        }
    }

    private interface Timed<T> {
        int ticks();

        T withTicks(int ticks);
    }

    private record BoundState(int ticks, Vec3 anchor) {
    }

    private record StunState(Vec3 anchor) {
    }

    private record WarpState(int ticks, GameType previousMode) {
    }

    private record TimedState(int ticks) implements Timed<TimedState> {
        @Override
        public TimedState withTicks(int ticks) {
            return new TimedState(ticks);
        }
    }

    private record WardState(int ticks, float strength) implements Timed<WardState> {
        @Override
        public WardState withTicks(int ticks) {
            return new WardState(ticks, strength);
        }
    }

    private record CharmState(int ticks, UUID caster) implements Timed<CharmState> {
        @Override
        public CharmState withTicks(int ticks) {
            return new CharmState(ticks, caster);
        }
    }

    private record HexState(int ticks, float damageMultiplier) implements Timed<HexState> {
        @Override
        public HexState withTicks(int ticks) {
            return new HexState(ticks, damageMultiplier);
        }
    }

    private record ManaShieldState(int ticks, float absorptionRatio) implements Timed<ManaShieldState> {
        @Override
        public ManaShieldState withTicks(int ticks) {
            return new ManaShieldState(ticks, absorptionRatio);
        }
    }

    private record LifeWardState(int ticks, float power) implements Timed<LifeWardState> {
        @Override
        public LifeWardState withTicks(int ticks) {
            return new LifeWardState(ticks, power);
        }
    }
}
