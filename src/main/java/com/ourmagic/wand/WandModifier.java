package com.ourmagic.wand;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

public record WandModifier(String key, String displayName, Item item) {
    public static final WandModifier HARDNESS = new WandModifier("hardness", "Hardness", Items.DIAMOND);
    public static final WandModifier HASTE = new WandModifier("haste", "Haste", Items.REDSTONE);
    public static final WandModifier FOCUS = new WandModifier("focus", "Focus", Items.QUARTZ);
    public static final WandModifier POTENCY = new WandModifier("potency", "Potency", Items.NETHERITE_INGOT);
    public static final WandModifier ELASTICITY = new WandModifier("elasticity", "Elasticity", Items.SLIME_BALL);
    public static final WandModifier WISDOM = new WandModifier("wisdom", "Wisdom", Items.LAPIS_LAZULI);

    private static final List<WandModifier> ALL = List.of(HARDNESS, HASTE, FOCUS, POTENCY, ELASTICITY, WISDOM);

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
