package com.ourmagic.mana;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class PlayerMana {
    private static final String ROOT_TAG = "OurMagicMana";
    private static final String TAG_MANA = "Mana";
    private static final String TAG_MAX_MANA = "MaxMana";
    private static final String TAG_REGEN = "Regen";

    public static final int DEFAULT_MAX_MANA = 50;
    public static final int DEFAULT_REGEN = 5;

    private final CompoundTag tag;

    private PlayerMana(CompoundTag tag) {
        this.tag = tag;
        initialize(tag);
    }

    public static PlayerMana get(Player player) {
        CompoundTag persisted = player.getPersistentData();
        if (!persisted.contains(ROOT_TAG, 10)) {
            persisted.put(ROOT_TAG, defaults());
        }
        return new PlayerMana(persisted.getCompound(ROOT_TAG));
    }

    public static void copy(Player original, Player target) {
        target.getPersistentData().put(ROOT_TAG, get(original).tag.copy());
    }

    public int mana() {
        return tag.getInt(TAG_MANA);
    }

    public int maxMana() {
        return tag.getInt(TAG_MAX_MANA);
    }

    public int regen() {
        return tag.getInt(TAG_REGEN);
    }

    public boolean has(int amount) {
        return mana() >= amount;
    }

    public boolean spend(int amount) {
        if (!has(amount)) {
            return false;
        }
        setMana(mana() - amount);
        return true;
    }

    public boolean regenerate() {
        int before = mana();
        setMana(Math.min(maxMana(), before + regen()));
        return mana() != before;
    }

    private void setMana(int mana) {
        tag.putInt(TAG_MANA, Math.max(0, Math.min(maxMana(), mana)));
    }

    private static void initialize(CompoundTag tag) {
        if (!tag.contains(TAG_MAX_MANA)) {
            tag.putInt(TAG_MAX_MANA, DEFAULT_MAX_MANA);
        }
        if (!tag.contains(TAG_REGEN)) {
            tag.putInt(TAG_REGEN, DEFAULT_REGEN);
        }
        if (!tag.contains(TAG_MANA)) {
            tag.putInt(TAG_MANA, tag.getInt(TAG_MAX_MANA));
        }
        if (tag.getInt(TAG_MANA) > tag.getInt(TAG_MAX_MANA)) {
            tag.putInt(TAG_MANA, tag.getInt(TAG_MAX_MANA));
        }
    }

    private static CompoundTag defaults() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_MAX_MANA, DEFAULT_MAX_MANA);
        tag.putInt(TAG_MANA, DEFAULT_MAX_MANA);
        tag.putInt(TAG_REGEN, DEFAULT_REGEN);
        return tag;
    }
}
