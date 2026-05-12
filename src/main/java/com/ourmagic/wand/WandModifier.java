package com.ourmagic.wand;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

public record WandModifier(String key, String displayName, Item item, int maxLevel) {
    // All leveled modifiers have a max level of 10
    public static final WandModifier HARDNESS = new WandModifier("hardness", "Hardness", Items.DIAMOND, 10);
    public static final WandModifier HASTE = new WandModifier("haste", "Haste", Items.REDSTONE, 10);
    public static final WandModifier FOCUS = new WandModifier("focus", "Focus", Items.QUARTZ, 10);
    public static final WandModifier POTENCY = new WandModifier("potency", "Potency", Items.NETHERITE_INGOT, 10);
    public static final WandModifier ELASTICITY = new WandModifier("elasticity", "Elasticity", Items.SLIME_BALL, 10);
    public static final WandModifier WISDOM = new WandModifier("wisdom", "Wisdom", Items.LAPIS_LAZULI, 10);
    public static final WandModifier Empty = new WandModifier("empty", "Empty", Items.GOLD_INGOT, 0);
    public static final WandModifier ENDURANCE = new WandModifier("endurance", "Endurance", Items.EMERALD, 10);

    private static final List<WandModifier> ALL = List.of(HARDNESS, HASTE, FOCUS, POTENCY, ELASTICITY, WISDOM, Empty, ENDURANCE);

    public static Optional<WandModifier> fromItem(ItemStack stack) {
        return ALL.stream().filter(modifier -> stack.is(modifier.item())).findFirst();
    }

    public static Optional<WandModifier> byKey(String key) {
        return ALL.stream().filter(modifier -> modifier.key().equals(key)).findFirst();
    }

    public static List<WandModifier> all() {
        return ALL;
    }
}
