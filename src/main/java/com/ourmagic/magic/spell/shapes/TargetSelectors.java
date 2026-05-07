package com.ourmagic.magic.spell.shapes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;

import java.util.Optional;

public final class TargetSelectors {
    private TargetSelectors() {
    }

    public static TargetSelector self() {
        return context -> Optional.of(SpellTarget.entity(context.player()));
    }

    public static TargetSelector lookedLiving(double range) {
        return context -> context.raycastEntity(range * context.data().power() * context.rangeMultiplier(), entity -> entity instanceof LivingEntity)
                .map(hit -> SpellTarget.entity(hit.getEntity()));
    }

    public static TargetSelector lookedLivingOrSelf(double range) {
        return context -> lookedLiving(range).select(context).or(() -> self().select(context));
    }

    public static TargetSelector lookedLivingOrPoint(double range, double missDistance) {
        return context -> context.raycastEntity(range * context.data().power() * context.rangeMultiplier(), entity -> entity instanceof LivingEntity)
                .map(hit -> SpellTarget.entity(hit.getEntity()))
                .or(() -> lookedPoint(range, missDistance).select(context));
    }

    public static TargetSelector lookedPoint(double range, double missDistance) {
        return context -> {
            HitResult hit = context.raycast(range * context.data().power() * context.rangeMultiplier());
            if (hit.getType() == HitResult.Type.MISS) {
                return Optional.of(SpellTarget.point(context.player().getEyePosition().add(context.player().getLookAngle().scale(missDistance))));
            }
            return Optional.of(SpellTarget.point(hit.getLocation()));
        };
    }

    public static TargetSelector lookedPointFromPlayerPosition(double range, double missDistance) {
        return context -> {
            HitResult hit = context.raycast(range * context.data().power() * context.rangeMultiplier());
            if (hit.getType() == HitResult.Type.MISS) {
                return Optional.of(SpellTarget.point(context.player().position().add(context.player().getLookAngle().scale(missDistance * context.data().power() * context.rangeMultiplier()))));
            }
            return Optional.of(SpellTarget.point(hit.getLocation()));
        };
    }

    public static TargetSelector lookedBlockAdjacent(double range) {
        return context -> {
            HitResult hit = context.raycast(range * context.data().power() * context.rangeMultiplier());
            if (hit instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                return Optional.of(SpellTarget.block(blockHit.getBlockPos().relative(blockHit.getDirection())));
            }
            return Optional.empty();
        };
    }
}
