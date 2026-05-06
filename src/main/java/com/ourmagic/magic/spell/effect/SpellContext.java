package com.ourmagic.magic.spell.effect;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public record SpellContext(Level level, ServerPlayer player, ItemStack wand, WandData data, Spell spell, int castIndex, int castCount, float modifierPower, boolean beamsEnabled, boolean areaCast, Optional<Vec3> areaOrigin) {
    public SpellContext(Level level, ServerPlayer player, ItemStack wand, WandData data, Spell spell) {
        this(level, player, wand, data, spell, 0, 1, 1.0F, true, false, Optional.empty());
    }

    public SpellContext withCastIteration(int castIndex, int castCount, float modifierPower) {
        return new SpellContext(level, player, wand, data, spell, castIndex, castCount, modifierPower, beamsEnabled, areaCast, areaOrigin);
    }

    public SpellContext withoutBeams() {
        return new SpellContext(level, player, wand, data, spell, castIndex, castCount, modifierPower, false, areaCast, areaOrigin);
    }

    public SpellContext asAreaCast(Vec3 origin) {
        return new SpellContext(level, player, wand, data, spell, castIndex, castCount, modifierPower, beamsEnabled, true, Optional.of(origin));
    }

    public float damagePower() {
        return data.power() * data.activeDamageMultiplier() * (1.0F + data.activeUpgradeLevel(Spell.UPGRADE_DAMAGE) * 0.10F) * modifierPower;
    }

    public float utilityPower() {
        return data.power() * data.activeUtilityMultiplier() * modifierPower;
    }

    public float rangeMultiplier() {
        return (1.0F + data.activeUpgradeLevel(Spell.UPGRADE_RANGE) * 0.20F) * data.rangeMultiplierFromWand();
    }

    public float radiusMultiplier() {
        return (1.0F + data.activeUpgradeLevel(Spell.UPGRADE_RADIUS) * 0.20F) * data.radiusMultiplierFromWand();
    }

    public float durationMultiplier() {
        return (1.0F + data.activeUpgradeLevel(Spell.UPGRADE_DURATION) * 0.20F) * data.durationMultiplier();
    }

    public boolean selfShape() {
        return SpellRegistry.shapeKey(spell.key()).contains("self");
    }

    public HitResult raycast(double distance) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(distance));
        return level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    public Optional<EntityHitResult> raycastEntity(double distance, Predicate<Entity> filter) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(distance));
        AABB bounds = player.getBoundingBox().expandTowards(look.scale(distance)).inflate(1.0D);
        EntityHitResult best = null;
        double bestDistance = distance * distance;

        for (Entity entity : level.getEntities(player, bounds, entity -> entity.isPickable() && filter.test(entity))) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius() + 0.35D);
            Optional<Vec3> hit = box.clip(eye, end);
            if (hit.isPresent()) {
                double candidate = eye.distanceToSqr(hit.get());
                if (candidate < bestDistance) {
                    bestDistance = candidate;
                    best = new EntityHitResult(entity, hit.get());
                }
            }
        }

        return Optional.ofNullable(best);
    }

    public Optional<LivingEntity> nearestLiving(Vec3 origin, double radius, Set<Integer> excludedIds) {
        AABB bounds = new AABB(origin, origin).inflate(radius);
        LivingEntity best = null;
        double bestDistance = radius * radius;
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, bounds, entity -> entity != player && entity.isAlive() && !excludedIds.contains(entity.getId()))) {
            double distance = entity.distanceToSqr(origin);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return Optional.ofNullable(best);
    }

    public void beam(Vec3 end, ParticleOptions particle) {
        if (!beamsEnabled) {
            return;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 start = player.getEyePosition().add(player.getLookAngle().scale(0.7D));
        Vec3 delta = end.subtract(start);
        int steps = Math.max(4, (int) (delta.length() * 3.0D));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = start.add(delta.scale(i / (double) steps));
            serverLevel.sendParticles(particle, at.x, at.y, at.z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
    }

    public void sparkleBeam(Vec3 end) {
        beam(end, ParticleTypes.END_ROD);
        beam(end, ParticleTypes.ENCHANT);
    }

    public void burst(Vec3 at, ParticleOptions particle, int count, double spread, double speed) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
        }
    }

    public void ring(Vec3 center, ParticleOptions particle, double radius, int count) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D * i) / count;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            serverLevel.sendParticles(particle, x, center.y, z, 1, 0.0D, 0.04D, 0.0D, 0.0D);
        }
    }
}
