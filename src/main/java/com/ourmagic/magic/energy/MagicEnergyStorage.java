package com.ourmagic.magic.energy;

import net.minecraft.nbt.CompoundTag;

public class MagicEnergyStorage {
    private final int capacity;
    private final int[] energy = new int[MagicEnergyType.values().length];

    public MagicEnergyStorage(int capacity) {
        this.capacity = Math.max(0, capacity);
    }

    public int capacity() {
        return capacity;
    }

    public int stored(MagicEnergyType type) {
        return energy[type.ordinal()];
    }

    public int receive(MagicEnergyType type, int amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, capacity - stored(type));
        if (accepted > 0 && !simulate) {
            energy[type.ordinal()] += accepted;
        }
        return accepted;
    }

    public int extract(MagicEnergyType type, int amount, boolean simulate) {
        if (amount <= 0) {
            return 0;
        }
        int extracted = Math.min(amount, stored(type));
        if (extracted > 0 && !simulate) {
            energy[type.ordinal()] -= extracted;
        }
        return extracted;
    }

    public void load(CompoundTag tag) {
        for (MagicEnergyType type : MagicEnergyType.values()) {
            energy[type.ordinal()] = Math.max(0, Math.min(capacity, tag.getInt(type.serializedName())));
        }
    }

    public void save(CompoundTag tag) {
        for (MagicEnergyType type : MagicEnergyType.values()) {
            int stored = stored(type);
            if (stored > 0) {
                tag.putInt(type.serializedName(), stored);
            }
        }
    }
}
