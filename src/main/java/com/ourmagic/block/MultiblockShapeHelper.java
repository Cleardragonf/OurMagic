package com.ourmagic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

final class MultiblockShapeHelper {
    private static final int MAX_BLOCKS = 128;

    private MultiblockShapeHelper() {
    }

    static VoxelShape connectedShape(BlockGetter level, BlockPos origin, Block block) {
        if (!level.getBlockState(origin).is(block)) {
            return Shapes.empty();
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        VoxelShape shape = Shapes.empty();
        queue.add(origin.immutable());

        while (!queue.isEmpty() && visited.size() < MAX_BLOCKS) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos.asLong()) || !level.getBlockState(pos).is(block)) {
                continue;
            }

            double minX = (pos.getX() - origin.getX()) * 16.0D;
            double minY = (pos.getY() - origin.getY()) * 16.0D;
            double minZ = (pos.getZ() - origin.getZ()) * 16.0D;
            shape = Shapes.or(shape, Block.box(minX, minY, minZ, minX + 16.0D, minY + 16.0D, minZ + 16.0D));

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (!visited.contains(neighbor.asLong()) && level.getBlockState(neighbor).is(block)) {
                    queue.add(neighbor.immutable());
                }
            }
        }

        return shape.optimize();
    }
}
