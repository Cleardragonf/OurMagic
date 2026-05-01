package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.item.ItemEntity;

public final class SpellShapes {
    private SpellShapes() {
    }

    public static SpellShape self() {
        return new SpellShape(TargetSelectors.self(), AreaSelectors.singleTarget());
    }

    public static SpellShape lookedLiving(double range) {
        return new SpellShape(TargetSelectors.lookedLiving(range), AreaSelectors.singleTarget());
    }

    public static SpellShape lookedLivingOrSelf(double range) {
        return new SpellShape(TargetSelectors.lookedLivingOrSelf(range), AreaSelectors.singleTarget());
    }

    public static SpellShape lookedLivingOrPoint(double range, double missDistance) {
        return new SpellShape(TargetSelectors.lookedLivingOrPoint(range, missDistance), AreaSelectors.singleTarget());
    }

    public static SpellShape lookedPoint(double range, double missDistance) {
        return new SpellShape(TargetSelectors.lookedPoint(range, missDistance), AreaSelectors.singleTarget());
    }

    public static SpellShape lookedPointFromPlayerPosition(double range, double missDistance) {
        return new SpellShape(TargetSelectors.lookedPointFromPlayerPosition(range, missDistance), AreaSelectors.singleTarget());
    }

    public static SpellShape lookedBlockAdjacent(double range) {
        return new SpellShape(TargetSelectors.lookedBlockAdjacent(range), AreaSelectors.singleTarget());
    }

    public static SpellShape playersAroundSelf(double radius, ParticleOptions areaRingParticle) {
        return new SpellShape(TargetSelectors.self(), AreaSelectors.playersAroundTarget(radius, 0, true), areaRingParticle);
    }

    public static SpellShape playersAroundLookedLivingOrSelf(double range, double radius, ParticleOptions areaRingParticle) {
        return new SpellShape(TargetSelectors.lookedLivingOrSelf(range), AreaSelectors.playersAroundTarget(radius, 0, true), areaRingParticle);
    }

    public static SpellShape livingAroundLookedPoint(double range, double missDistance, double radius, int maxExtraTargets, ParticleOptions areaRingParticle) {
        return new SpellShape(TargetSelectors.lookedLivingOrPoint(range, missDistance), AreaSelectors.livingAroundTarget(radius, false, maxExtraTargets, true), areaRingParticle);
    }

    public static SpellShape waterBlocksAroundLookedLivingOrPoint(double range, double missDistance, int horizontalRadius, int down, int up, ParticleOptions areaRingParticle) {
        return new SpellShape(TargetSelectors.lookedLivingOrPoint(range, missDistance), AreaSelectors.waterBlocksAroundTarget(horizontalRadius, down, up, true), areaRingParticle);
    }

    public static SpellShape itemsAroundSelf(double radius, ParticleOptions areaRingParticle) {
        return new SpellShape(TargetSelectors.self(), AreaSelectors.entitiesAroundTarget(radius, false, 0, true, entity -> entity instanceof ItemEntity), areaRingParticle);
    }
}
