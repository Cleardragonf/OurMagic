package com.ourmagic.magic.spell.shapes;

import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.runtime.MagicAllies;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

public final class AreaSelectors {
    private AreaSelectors() {
    }

    public static AreaSelector singleTarget() {
        return new AreaSelector() {
            @Override
            public List<SpellTarget> select(SpellContext context, SpellTarget origin) {
                return List.of(origin);
            }

            @Override
            public double radius(SpellContext context) {
                return 0.0D;
            }
        };
    }

    public static AreaSelector playersAroundTarget(double baseRadius, int maxExtraTargets, boolean includeOrigin) {
        return entitiesAroundTarget(baseRadius, true, maxExtraTargets, includeOrigin, entity -> entity instanceof Player);
    }

    public static AreaSelector alliesAroundTarget(double baseRadius, int maxExtraTargets, boolean includeOrigin) {
        return new AreaSelector() {
            @Override
            public List<SpellTarget> select(SpellContext context, SpellTarget origin) {
                double radius = radius(context);
                List<SpellTarget> targets = new ArrayList<>();
                origin.entity()
                        .filter(entity -> includeOrigin && MagicAllies.isAlly(context.player(), entity))
                        .ifPresent(entity -> targets.add(SpellTarget.entity(entity)));
                if (radius <= 0.0D) {
                    return targets;
                }

                AABB bounds = new AABB(origin.position(), origin.position()).inflate(radius);
                List<LivingEntity> candidates = new ArrayList<>(context.level().getEntitiesOfClass(LivingEntity.class, bounds, entity -> entity.isAlive() && MagicAllies.isAlly(context.player(), entity)));
                origin.entity().ifPresent(originEntity -> candidates.removeIf(candidate -> candidate == originEntity));
                Collections.shuffle(candidates, new Random(context.player().getRandom().nextLong()));

                int limit = maxExtraTargets <= 0 ? candidates.size() : Math.min(maxExtraTargets, candidates.size());
                for (int i = 0; i < limit; i++) {
                    targets.add(SpellTarget.entity(candidates.get(i)));
                }
                return targets;
            }

            @Override
            public double radius(SpellContext context) {
                return baseRadius * context.data().power() * context.data().activeUtilityMultiplier() * context.radiusMultiplier();
            }
        };
    }

    public static AreaSelector livingAroundTarget(double baseRadius, boolean scaleWithRadiusUpgrade, int maxExtraTargets, boolean includeOrigin) {
        return entitiesAroundTarget(baseRadius, scaleWithRadiusUpgrade, maxExtraTargets, includeOrigin, entity -> entity instanceof net.minecraft.world.entity.LivingEntity);
    }

    public static AreaSelector entitiesAroundTarget(double baseRadius, boolean scaleWithRadiusUpgrade, int maxExtraTargets, boolean includeOrigin, Predicate<Entity> filter) {
        return new AreaSelector() {
            @Override
            public List<SpellTarget> select(SpellContext context, SpellTarget origin) {
                double radius = radius(context);
                List<SpellTarget> targets = new ArrayList<>();
                if (includeOrigin) {
                    targets.add(origin);
                }
                if (radius <= 0.0D) {
                    return targets;
                }

                AABB bounds = new AABB(origin.position(), origin.position()).inflate(radius);
                List<Entity> candidates = new ArrayList<>(context.level().getEntities(context.player(), bounds, entity -> entity.isAlive() && filter.test(entity)));
                origin.entity().ifPresent(originEntity -> candidates.removeIf(candidate -> candidate == originEntity));
                Collections.shuffle(candidates, new Random(context.player().getRandom().nextLong()));

                int limit = maxExtraTargets <= 0 ? candidates.size() : Math.min(maxExtraTargets, candidates.size());
                for (int i = 0; i < limit; i++) {
                    targets.add(SpellTarget.entity(candidates.get(i)));
                }
                return targets;
            }

            @Override
            public double radius(SpellContext context) {
                double radius = baseRadius * context.data().power() * context.data().activeUtilityMultiplier();
                return scaleWithRadiusUpgrade ? radius * context.radiusMultiplier() : radius;
            }
        };
    }

    public static AreaSelector waterBlocksAroundTarget(int horizontalRadius, int down, int up, boolean includeOrigin) {
        return new AreaSelector() {
            @Override
            public List<SpellTarget> select(SpellContext context, SpellTarget origin) {
                List<SpellTarget> targets = new ArrayList<>();
                if (includeOrigin) {
                    targets.add(origin);
                }

                BlockPos center = BlockPos.containing(origin.position());
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-horizontalRadius, -down, -horizontalRadius), center.offset(horizontalRadius, up, horizontalRadius))) {
                    if (context.level().getBlockState(pos).is(Blocks.WATER)) {
                        targets.add(SpellTarget.block(pos.immutable()));
                    }
                }
                return targets;
            }

            @Override
            public double radius(SpellContext context) {
                return horizontalRadius;
            }
        };
    }
}
