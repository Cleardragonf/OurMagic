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
    String UPGRADE_ENTITIES = "entities";
    String UPGRADE_CASTS = "casts";

    String key();

    int manaCost();

    int cooldownTicks();

    boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data);

    default boolean supportsUpgrade(String upgrade) {
        return false;
    }

    default boolean supportsUpgradeFamily(String upgrade) {
        return supportsUpgrade(upgrade);
    }
}
