package com.ourmagic.wand;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

/**
 * Material modifiers are single-use modifiers that can be applied to wands.
 * They provide specific effects and occupy 1 upgrade slot each.
 * Unlike leveled modifiers, material modifiers cannot be stacked beyond 1 level.
 */
public record WandMaterialModifier(String key, String displayName, Item item, String effect) {
    // Woods - each provides -1 second cooldown
    public static final WandMaterialModifier CHERRY = new WandMaterialModifier("cherry_wood", "Cherry", Items.CHERRY_LOG, "-1 sec cooldown");
    public static final WandMaterialModifier OAK = new WandMaterialModifier("oak_wood", "Oak", Items.OAK_LOG, "-1 sec cooldown");
    public static final WandMaterialModifier SPRUCE = new WandMaterialModifier("spruce_wood", "Spruce", Items.SPRUCE_LOG, "-1 sec cooldown");
    public static final WandMaterialModifier BIRCH = new WandMaterialModifier("birch_wood", "Birch", Items.BIRCH_LOG, "-1 sec cooldown");
    public static final WandMaterialModifier JUNGLE = new WandMaterialModifier("jungle_wood", "Jungle", Items.JUNGLE_LOG, "-1 sec cooldown");
    public static final WandMaterialModifier ACACIA = new WandMaterialModifier("acacia_wood", "Acacia", Items.ACACIA_LOG, "-1 sec cooldown");
    public static final WandMaterialModifier DARK_OAK = new WandMaterialModifier("dark_oak_wood", "Dark Oak", Items.DARK_OAK_LOG, "-1 sec cooldown");
    public static final WandMaterialModifier MANGROVE = new WandMaterialModifier("mangrove_wood", "Mangrove", Items.MANGROVE_LOG, "-1 sec cooldown");
    public static final WandMaterialModifier PALE_OAK = new WandMaterialModifier("pale_oak_wood", "Pale Oak", Items.PALE_OAK_LOG, "-1 sec cooldown");

    // Mob Drops
    public static final WandMaterialModifier STRING = new WandMaterialModifier("string", "String", Items.STRING, "-0.5 sec cooldown");
    public static final WandMaterialModifier BONE = new WandMaterialModifier("bone", "Bone", Items.BONE, "+10% spell duration");
    public static final WandMaterialModifier ROTTEN_FLESH = new WandMaterialModifier("rotten_flesh", "Rotten Flesh", Items.ROTTEN_FLESH, "+5% spell damage");
    public static final WandMaterialModifier GUNPOWDER = new WandMaterialModifier("gunpowder", "Gunpowder", Items.GUNPOWDER, "+0.5 sec cooldown (risk/reward)");
    public static final WandMaterialModifier ENDER_PEARL = new WandMaterialModifier("ender_pearl", "Ender Pearl", Items.ENDER_PEARL, "+5% spell range");
    public static final WandMaterialModifier SPIDER_EYE = new WandMaterialModifier("spider_eye", "Spider Eye", Items.SPIDER_EYE, "+3% spell potency");
    public static final WandMaterialModifier PHANTOM_MEMBRANE = new WandMaterialModifier("phantom_membrane", "Phantom Membrane", Items.PHANTOM_MEMBRANE, "+2% haste (faster cast)");

    private static final List<WandMaterialModifier> ALL = List.of(
            CHERRY, OAK, SPRUCE, BIRCH, JUNGLE, ACACIA, DARK_OAK, MANGROVE, PALE_OAK,
            STRING, BONE, ROTTEN_FLESH, GUNPOWDER, ENDER_PEARL, SPIDER_EYE, PHANTOM_MEMBRANE
    );

    public static Optional<WandMaterialModifier> fromItem(ItemStack stack) {
        return ALL.stream().filter(modifier -> stack.is(modifier.item())).findFirst();
    }

    public static Optional<WandMaterialModifier> byKey(String key) {
        return ALL.stream().filter(modifier -> modifier.key().equals(key)).findFirst();
    }

    public static List<WandMaterialModifier> all() {
        return ALL;
    }
}
