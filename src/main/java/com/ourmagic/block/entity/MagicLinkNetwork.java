package com.ourmagic.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

final class MagicLinkNetwork {
    private static final int SCAN_RADIUS = 32;

    private MagicLinkNetwork() {
    }

    static int inboundLinkCount(ServerLevel level, BlockPos target) {
        int links = 0;
        BlockPos.MutableBlockPos scan = new BlockPos.MutableBlockPos();
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -SCAN_RADIUS; y <= SCAN_RADIUS; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    scan.set(target.getX() + x, target.getY() + y, target.getZ() + z);
                    if (scan.equals(target)) {
                        continue;
                    }
                    BlockEntity blockEntity = level.getBlockEntity(scan);
                    if (linksTo(blockEntity, target)) {
                        links++;
                    }
                }
            }
        }
        return links;
    }

    static boolean targetHasLinkCapacity(ServerLevel level, BlockPos target) {
        BlockEntity blockEntity = level.getBlockEntity(target);
        if (blockEntity instanceof MagicRelayBlockEntity relay) {
            return relay.totalLinkCount(level) < relay.maxLinks();
        }
        if (blockEntity instanceof MagicBatteryBlockEntity battery) {
            return battery.totalLinkCount(level) < battery.maxLinks();
        }
        if (blockEntity instanceof MagicAccumulatorBlockEntity accumulator) {
            return accumulator.totalLinkCount(level) < accumulator.maxLinks();
        }
        return true;
    }

    private static boolean linksTo(BlockEntity blockEntity, BlockPos target) {
        if (blockEntity instanceof MagicAccumulatorBlockEntity accumulator) {
            return accumulator.linksTo(target);
        }
        if (blockEntity instanceof MagicBatteryBlockEntity battery) {
            return battery.linksTo(target);
        }
        if (blockEntity instanceof MagicRelayBlockEntity relay) {
            return relay.linksTo(target);
        }
        return false;
    }
}
