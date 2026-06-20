package com.ourmagic.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientQuantumStorageData {
    private static final Map<BlockPos, Data> DATA = new HashMap<>();

    private ClientQuantumStorageData() {
    }

    public static void set(BlockPos corePos, Summary summary, List<Entry> entries) {
        DATA.put(corePos.immutable(), new Data(summary, List.copyOf(entries)));
    }

    public static Data get(BlockPos corePos) {
        return DATA.getOrDefault(corePos, new Data(new Summary(0, 0, 0, 0), List.of()));
    }

    public record Data(Summary summary, List<Entry> entries) {
    }

    public record Summary(int bays, int drives, int used, int capacity) {
    }

    public record Entry(int index, ItemStack stack, int count, int capacity) {
    }
}
