package com.ourmagic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public class TemporaryShieldBlock extends Block {
    private static final Map<GlobalPos, BlockState> REPLACED_BLOCKS = new HashMap<>();

    public TemporaryShieldBlock(Properties properties) {
        super(properties);
    }

    public static boolean place(ServerLevel level, BlockPos pos, BlockState shieldState, int lifetimeTicks) {
        BlockState state = level.getBlockState(pos);
        if (state.is(shieldState.getBlock())) {
            level.scheduleTick(pos, shieldState.getBlock(), lifetimeTicks);
            return true;
        }
        if (!state.isAir() && state.isCollisionShapeFullBlock(level, pos)) {
            return false;
        }

        GlobalPos key = GlobalPos.of(level.dimension(), pos);
        if (!state.isAir()) {
            REPLACED_BLOCKS.putIfAbsent(key, state);
        }
        level.setBlock(pos, shieldState, Block.UPDATE_ALL);
        level.scheduleTick(pos, shieldState.getBlock(), lifetimeTicks);
        return true;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState replaced = REPLACED_BLOCKS.remove(GlobalPos.of(level.dimension(), pos));
        if (replaced == null) {
            level.removeBlock(pos, false);
        } else {
            level.setBlock(pos, replaced, Block.UPDATE_ALL);
        }
    }
}
