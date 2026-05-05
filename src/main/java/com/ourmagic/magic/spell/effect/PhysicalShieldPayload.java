package com.ourmagic.magic.spell.effect;

import com.ourmagic.block.TemporaryShieldBlock;
import com.ourmagic.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PhysicalShieldPayload implements PayloadEffect {
    private static final int SELF_SIZE = 3;
    private static final int TARGET_SIZE = 5;
    private static final int LIFETIME_TICKS = 80;

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (!(context.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Direction forward = horizontalFacing(context.player().getLookAngle());
        Direction right = forward.getClockWise();
        boolean selfCast = target.entity().filter(entity -> entity == context.player()).isPresent();
        int size = selfCast ? SELF_SIZE : TARGET_SIZE;
        BlockPos center = selfCast
                ? context.player().blockPosition().relative(forward, 3).above(1)
                : target.block().orElse(BlockPos.containing(target.position())).above(size / 2);

        int half = size / 2;
        int placed = 0;
        for (int width = -half; width <= half; width++) {
            for (int height = -half; height <= half; height++) {
                BlockPos pos = center.relative(right, width).above(height);
                if (placeShield(serverLevel, pos)) {
                    placed++;
                }
            }
        }

        if (placed > 0) {
            context.ring(center.getCenter(), ParticleTypes.ENCHANT, size * 0.45D, 28);
            context.burst(center.getCenter(), ParticleTypes.END_ROD, 18, size * 0.35D, 0.02D);
        }
        return placed > 0;
    }

    private static boolean placeShield(ServerLevel level, BlockPos pos) {
        Block shield = ModBlocks.TEMPORARY_SHIELD.get();
        return TemporaryShieldBlock.place(level, pos, shield.defaultBlockState(), LIFETIME_TICKS);
    }

    private static Direction horizontalFacing(Vec3 look) {
        Direction direction = Direction.getNearest(look.x, 0.0D, look.z);
        return direction.getAxis().isHorizontal() ? direction : Direction.NORTH;
    }
}
