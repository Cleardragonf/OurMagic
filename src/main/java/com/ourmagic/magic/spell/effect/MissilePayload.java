package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Optional;

public class MissilePayload implements PayloadEffect {
    public static final DustParticleOptions PURPLE_PARTICLE = new DustParticleOptions(new Vector3f(0.62F, 0.16F, 1.0F), 1.15F);
    private static final int MAX_FLIGHT_TICKS = 32;
    private static final double SPEED = 0.72D;
    private static final double HIT_RADIUS = 0.75D;

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (!(context.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Vec3 targetPos = overheadTarget(context, target);
        Vec3 start = context.selfShape() || context.areaCast() && target.entity().isPresent() && target.entity().get() != context.player()
                ? targetPos.add(0.0D, 7.5D, 0.0D)
                : context.player().getEyePosition().add(context.player().getLookAngle().scale(0.65D));
        Vec3 fallbackEnd = targetPos;
        if (fallbackEnd.distanceToSqr(start) < 0.001D) {
            fallbackEnd = start.add(context.player().getLookAngle().scale(18.0D));
        }

        Optional<LivingEntity> targetEntity = target.entity()
                .filter(entity -> entity instanceof LivingEntity)
                .map(entity -> (LivingEntity) entity)
                .filter(entity -> entity != context.player());
        fly(serverLevel, context, start, fallbackEnd, targetEntity, 0);

        if (context.castIndex() == 0) {
            context.burst(start, PURPLE_PARTICLE, 10, 0.12D, 0.02D);
        }
        return true;
    }

    private static Vec3 overheadTarget(SpellContext context, SpellTarget target) {
        return target.entity().filter(entity -> entity == context.player()).isPresent()
                ? context.player().getEyePosition().add(context.player().getLookAngle().scale(8.0D))
                : target.position();
    }

    private static void fly(ServerLevel level, SpellContext context, Vec3 position, Vec3 fallbackEnd, Optional<LivingEntity> targetEntity, int ticks) {
        Vec3 destination = targetEntity
                .filter(Entity::isAlive)
                .map(entity -> entity.position().add(0.0D, entity.getBbHeight() * 0.55D, 0.0D))
                .orElse(fallbackEnd);
        Vec3 delta = destination.subtract(position);
        double distance = delta.length();

        drawBolt(level, position, delta);

        if (distance <= HIT_RADIUS || ticks >= MAX_FLIGHT_TICKS) {
            targetEntity.filter(Entity::isAlive).ifPresent(entity -> {
                entity.hurt(level.damageSources().magic(), (float) (6.0D * context.damagePower()));
                level.sendParticles(ParticleTypes.WITCH, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 18, 0.35D, 0.35D, 0.35D, 0.05D);
                level.sendParticles(PURPLE_PARTICLE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 16, 0.3D, 0.3D, 0.3D, 0.02D);
            });
            if (targetEntity.isEmpty()) {
                level.sendParticles(PURPLE_PARTICLE, destination.x, destination.y, destination.z, 12, 0.22D, 0.22D, 0.22D, 0.02D);
            }
            return;
        }

        Vec3 direction = delta.normalize();
        Vec3 next = position.add(direction.scale(Math.min(SPEED * context.data().power(), distance)));
        DelayedSpellCasts.schedule(level, 1, () -> fly(level, context, next, fallbackEnd, targetEntity, ticks + 1));
    }

    private static void drawBolt(ServerLevel level, Vec3 position, Vec3 delta) {
        Vec3 direction = delta.lengthSqr() < 0.001D ? Vec3.ZERO : delta.normalize();
        for (int i = 0; i < 4; i++) {
            Vec3 at = position.subtract(direction.scale(i * 0.16D));
            level.sendParticles(PURPLE_PARTICLE, at.x, at.y, at.z, 1, 0.025D, 0.025D, 0.025D, 0.0D);
        }
        level.sendParticles(ParticleTypes.WITCH, position.x, position.y, position.z, 1, 0.015D, 0.015D, 0.015D, 0.0D);
    }
}
