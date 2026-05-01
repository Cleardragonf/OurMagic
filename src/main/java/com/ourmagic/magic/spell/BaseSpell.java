package com.ourmagic.magic.spell;

import com.ourmagic.magic.Spell;
import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public abstract class BaseSpell implements Spell {
    private final String key;
    private final int manaCost;
    private final int cooldownTicks;
    private final Set<String> supportedUpgrades;

    protected BaseSpell(String key, int manaCost, int cooldownTicks, String... supportedUpgrades) {
        this.key = key;
        this.manaCost = manaCost;
        this.cooldownTicks = cooldownTicks;
        this.supportedUpgrades = Set.of(supportedUpgrades);
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public int manaCost() {
        return manaCost;
    }

    @Override
    public int cooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public boolean supportsUpgrade(String upgrade) {
        return supportedUpgrades.contains(upgrade)
                || supportedUpgrades.contains(Spell.UPGRADE_CHAINING) && upgrade.startsWith(Spell.UPGRADE_CHAINING + ".")
                || supportedUpgrades.contains(Spell.UPGRADE_MULTISTRIKE) && upgrade.startsWith(Spell.UPGRADE_MULTISTRIKE + ".");
    }

    @Override
    public boolean supportsUpgradeFamily(String upgrade) {
        return supportedUpgrades.contains(upgrade);
    }

    protected int multistrikeCasts(WandData data) {
        return 1 + data.activeUpgradeLevel(Spell.UPGRADE_MULTISTRIKE) + data.activeUpgradeLevel(Spell.UPGRADE_MULTISTRIKE_CASTS);
    }

    protected float multistrikePowerMultiplier(WandData data) {
        return 1.0F + data.activeUpgradeLevel(Spell.UPGRADE_MULTISTRIKE_POWER) * 0.08F;
    }

    protected Vec3 multistrikeTarget(ServerPlayer player, Vec3 target, int strikeIndex) {
        if (strikeIndex == 0) {
            return target;
        }
        double spread = 0.7D + strikeIndex * 0.2D;
        return target.add((player.getRandom().nextDouble() - 0.5D) * spread, 0, (player.getRandom().nextDouble() - 0.5D) * spread);
    }

    protected float rangeMultiplier(WandData data) {
        return 1.0F + data.activeUpgradeLevel(Spell.UPGRADE_RANGE) * 0.20F;
    }

    protected float durationMultiplier(WandData data) {
        return 1.0F + data.activeUpgradeLevel(Spell.UPGRADE_DURATION) * 0.20F;
    }

    protected HitResult raycast(ServerPlayer player, double distance) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(distance));
        return player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    protected Optional<EntityHitResult> raycastEntity(ServerPlayer player, double distance, Predicate<Entity> filter) {
        Level level = player.level();
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

    protected boolean castOnLivingTarget(Level level, ServerPlayer player, WandData data, double distance, ParticleOptions chainParticle, BiConsumer<LivingEntity, Vec3> effect) {
        return raycastEntity(player, distance, entity -> entity instanceof LivingEntity)
                .map(hit -> {
                    LivingEntity target = (LivingEntity) hit.getEntity();
                    Vec3 targetPos = hit.getLocation();
                    effect.accept(target, targetPos);
                    chainLivingTargets(level, player, target, data, chainParticle, effect);
                    return true;
                })
                .orElse(false);
    }

    protected void chainLivingTargets(Level level, ServerPlayer player, LivingEntity firstTarget, WandData data, ParticleOptions particle, BiConsumer<LivingEntity, Vec3> effect) {
        Vec3 origin = firstTarget.position().add(0, firstTarget.getBbHeight() * 0.5D, 0);
        Set<Integer> hitEntities = new HashSet<>();
        hitEntities.add(firstTarget.getId());
        chainLivingTargetsFrom(level, player, origin, data, particle, hitEntities, effect);
    }

    protected void chainLivingTargetsFrom(Level level, ServerPlayer player, Vec3 firstTarget, WandData data, ParticleOptions particle, BiConsumer<LivingEntity, Vec3> effect) {
        chainLivingTargetsFrom(level, player, firstTarget, data, particle, new HashSet<>(), effect);
    }

    private void chainLivingTargetsFrom(Level level, ServerPlayer player, Vec3 firstTarget, WandData data, ParticleOptions particle, Set<Integer> hitEntities, BiConsumer<LivingEntity, Vec3> effect) {
        int chainLevel = data.activeUpgradeLevel(Spell.UPGRADE_CHAINING) + data.activeUpgradeLevel(Spell.UPGRADE_CHAINING_ENTITIES);
        if (!supportsUpgradeFamily(Spell.UPGRADE_CHAINING) || chainLevel <= 0 || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 origin = firstTarget;
        double radius = 4.0D + chainLevel + data.activeUpgradeLevel(Spell.UPGRADE_CHAINING_RADIUS);

        for (int i = 0; i < chainLevel; i++) {
            Optional<LivingEntity> next = nearestLiving(level, player, origin, radius, hitEntities);
            if (next.isEmpty()) {
                return;
            }

            LivingEntity target = next.get();
            hitEntities.add(target.getId());
            Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5D, 0);
            serverLevel.sendParticles(particle, origin.x, origin.y, origin.z, 10, 0.18D, 0.18D, 0.18D, 0.03D);
            serverLevel.sendParticles(particle, targetPos.x, targetPos.y, targetPos.z, 18, 0.35D, 0.35D, 0.35D, 0.02D);
            effect.accept(target, targetPos);
            origin = targetPos;
        }
    }

    private Optional<LivingEntity> nearestLiving(Level level, ServerPlayer player, Vec3 origin, double radius, Set<Integer> excludedIds) {
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

    protected void beam(ServerPlayer player, Vec3 end, ParticleOptions particle) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
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

    protected void sparkleBeam(ServerPlayer player, Vec3 end) {
        beam(player, end, ParticleTypes.END_ROD);
        beam(player, end, ParticleTypes.ENCHANT);
    }

    protected void burst(Level level, Vec3 at, ParticleOptions particle, int count, double spread, double speed) {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
        }
    }

    protected void ring(Level level, Vec3 center, ParticleOptions particle, double radius, int count) {
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
