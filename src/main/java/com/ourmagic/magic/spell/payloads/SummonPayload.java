package com.ourmagic.magic.spell.payloads;

import com.ourmagic.OurMagic;
import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public class SummonPayload implements PayloadEffect {
    private static final List<SummonedEntity> SUMMONS = new ArrayList<>();
    private static final int BASE_LIFETIME_TICKS = 20 * 30;
    private final Variant variant;

    public SummonPayload() {
        this(Variant.RANDOM);
    }

    public SummonPayload(Variant variant) {
        this.variant = variant;
    }

    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return switch (variant) {
            case UNDEAD -> ParticleTypes.SOUL;
            case BEAST -> ParticleTypes.CLOUD;
            case GUARDIAN -> ParticleTypes.END_ROD;
            case ARCANE -> ParticleTypes.WITCH;
            case SWARM -> ParticleTypes.POOF;
            default -> ParticleTypes.ENCHANT;
        };
    }

    @Override
    public java.util.Set<String> supportedUpgrades(SpellBuildContext context) {
        return java.util.Set.of(Spell.UPGRADE_MULTISTRIKE, Spell.UPGRADE_DURATION, Spell.UPGRADE_DAMAGE);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (!(context.level() instanceof ServerLevel level)) {
            return false;
        }

        int lifetime = Math.round(BASE_LIFETIME_TICKS * context.durationMultiplier());
        Vec3 center = target.position();
        boolean spawned = false;
        Mob mob = createMob(level, context.player().getRandom(), variant);
        if (mob != null) {
            Vec3 position = scatter(center, context.castIndex(), context.castCount());
            mob.moveTo(position.x, position.y, position.z, context.player().getYRot() + 180.0F, 0.0F);
            mob.setCustomName(Component.literal("Summoned " + title(mob.getType().getDescription().getString())));
            mob.setPersistenceRequired();
            empower(mob, context);
            level.addFreshEntity(mob);
            SUMMONS.add(new SummonedEntity(level, mob.getUUID(), context.player().getUUID(), lifetime, context.damagePower(), variant));
            spawned = true;
        }

        if (spawned) {
            context.ring(center, particle(new SpellBuildContext(context.spell().key(), "", "", List.of(), 0)), 1.2D, 40);
            context.burst(center, ParticleTypes.ENCHANT, 35, 0.7D, 0.05D);
        }
        return spawned;
    }

    private static Mob createMob(ServerLevel level, RandomSource random, Variant variant) {
        EntityType<? extends Mob> type = switch (variant) {
            case UNDEAD -> randomFrom(random, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.STRAY, EntityType.ZOMBIE_VILLAGER);
            case BEAST -> randomFrom(random, EntityType.WOLF, EntityType.SPIDER, EntityType.FOX);
            case GUARDIAN -> randomFrom(random, EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM);
            case ARCANE -> randomFrom(random, EntityType.WITCH, EntityType.VEX);
            case SWARM -> randomFrom(random, EntityType.CAVE_SPIDER, EntityType.SILVERFISH, EntityType.ENDERMITE);
            case RANDOM -> randomFrom(random, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.WOLF, EntityType.SPIDER, EntityType.WITCH, EntityType.SNOW_GOLEM, EntityType.VEX);
        };
        return type.create(level);
    }

    @SafeVarargs
    private static EntityType<? extends Mob> randomFrom(RandomSource random, EntityType<? extends Mob>... types) {
        return types[random.nextInt(types.length)];
    }

    private static Vec3 scatter(Vec3 center, int index, int count) {
        if (count <= 1) {
            return center;
        }
        double angle = Math.PI * 2.0D * index / count;
        double radius = 1.2D + Math.min(3.0D, count * 0.15D);
        return center.add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
    }

    private static void empower(Mob mob, SpellContext context) {
        int duration = Math.round(BASE_LIFETIME_TICKS * context.durationMultiplier());
        mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0));
        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, Math.max(0, context.data().activeUpgradeLevel(Spell.UPGRADE_DAMAGE) / 2)));
        if (mob instanceof Zombie || mob instanceof ZombieVillager || mob instanceof Skeleton || mob instanceof Stray || mob instanceof Spider || mob instanceof Vex || mob instanceof Witch) {
            mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, Math.max(0, context.data().activeUpgradeLevel(Spell.UPGRADE_DAMAGE))));
        }
        if (mob instanceof Wolf wolf) {
            wolf.setTame(true);
            wolf.setOwnerUUID(context.player().getUUID());
        }
        if (mob instanceof Skeleton skeleton) {
            skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        }
        if (mob instanceof Zombie zombie) {
            zombie.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SUMMONS.isEmpty()) {
            return;
        }

        Iterator<SummonedEntity> iterator = SUMMONS.iterator();
        while (iterator.hasNext()) {
            SummonedEntity summon = iterator.next();
            LivingEntity entity = findSummon(summon);
            if (entity == null || !entity.isAlive() || summon.ticks-- <= 0) {
                if (entity != null) {
                    summon.level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 25, 0.4D, 0.4D, 0.4D, 0.03D);
                    entity.discard();
                }
                iterator.remove();
                continue;
            }

            if (entity.tickCount % 20 == 0) {
                tickSpecial(summon, entity);
            }
            if (entity instanceof Mob mob && mob.getTarget() == null) {
                nearestEnemy(summon, entity).ifPresent(mob::setTarget);
            }
        }
    }

    private static LivingEntity findSummon(SummonedEntity summon) {
        return summon.level.getEntity(summon.entity) instanceof LivingEntity living ? living : null;
    }

    private static java.util.Optional<LivingEntity> nearestEnemy(SummonedEntity summon, LivingEntity entity) {
        AABB area = entity.getBoundingBox().inflate(10.0D);
        return summon.level.getEntitiesOfClass(LivingEntity.class, area, candidate -> candidate.isAlive() && !candidate.getUUID().equals(summon.owner) && !candidate.getUUID().equals(summon.entity) && !(candidate instanceof Villager))
                .stream()
                .min(java.util.Comparator.comparingDouble(candidate -> candidate.distanceToSqr(entity)));
    }

    private static void tickSpecial(SummonedEntity summon, LivingEntity entity) {
        summon.level.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.65D, entity.getZ(), 4, 0.25D, 0.25D, 0.25D, 0.01D);
        if (summon.variant == Variant.ARCANE && entity.getRandom().nextFloat() < 0.45F) {
            nearestEnemy(summon, entity).ifPresent(target -> {
                Vec3 direction = target.position().add(0, target.getBbHeight() * 0.5D, 0).subtract(entity.position().add(0, entity.getBbHeight() * 0.5D, 0)).normalize();
                SmallFireball fireball = new SmallFireball(summon.level, entity, direction.x, direction.y, direction.z);
                fireball.setPos(entity.getX(), entity.getY() + entity.getBbHeight() * 0.65D, entity.getZ());
                summon.level.addFreshEntity(fireball);
            });
        }
        if (summon.variant == Variant.GUARDIAN) {
            entity.heal(0.5F * summon.power);
        }
    }

    private static String title(String value) {
        if (value.isBlank()) {
            return "Ally";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    public enum Variant {
        RANDOM,
        UNDEAD,
        BEAST,
        GUARDIAN,
        ARCANE,
        SWARM
    }

    private static final class SummonedEntity {
        private final ServerLevel level;
        private final UUID entity;
        private final UUID owner;
        private final float power;
        private final Variant variant;
        private int ticks;

        private SummonedEntity(ServerLevel level, UUID entity, UUID owner, int ticks, float power, Variant variant) {
            this.level = level;
            this.entity = entity;
            this.owner = owner;
            this.ticks = ticks;
            this.power = power;
            this.variant = variant;
        }
    }
}
