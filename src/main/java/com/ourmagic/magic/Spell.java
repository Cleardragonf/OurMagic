package com.ourmagic.magic;

import com.ourmagic.wand.WandData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface Spell {
    String UPGRADE_CHAINING = "chaining";
    String UPGRADE_MULTISTRIKE = "multistrike";
    String UPGRADE_DAMAGE = "damage";
    String UPGRADE_RANGE = "range";
    String UPGRADE_RADIUS = "radius";
    String UPGRADE_DURATION = "duration";
    String UPGRADE_TARGETING = "targeting";
    String UPGRADE_ENTITIES = "chaining.entities";
    String UPGRADE_CHAIN_RADIUS = "chaining.radius";
    String UPGRADE_CHAIN_DAMAGE = "chaining.damage";
    String UPGRADE_CASTS = "multistrike.casts";
    String UPGRADE_EXPLOSION_POWER = "explosion.power";
    String UPGRADE_EXPLOSION_RADIUS = "explosion.radius";

    String key();

    int manaCost();

    default int minManaCost() {
        return manaCost();
    }

    default int maxManaCost() {
        return manaCost();
    }

    int cooldownTicks();

    default int minCooldownTicks() {
        return cooldownTicks();
    }

    default int maxCooldownTicks() {
        return cooldownTicks();
    }

    default boolean isPhysical() {
        return false;
    }

    default boolean supportsChanting() {
        return true;
    }

    boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data);

    default FocusEffect focusEffect() {
        return FocusEffect.UTILITY;
    }

    default boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data, float focusMultiplier) {
        return cast(level, player, wand, data);
    }

    default boolean supportsUpgrade(String upgrade) {
        return false;
    }

    default boolean supportsUpgradeFamily(String upgrade) {
        return supportsUpgrade(upgrade);
    }

    enum FocusEffect {
        NONE,
        DAMAGE,
        UTILITY,
        DURATION,
        RANGE,
        RADIUS
    }
}
