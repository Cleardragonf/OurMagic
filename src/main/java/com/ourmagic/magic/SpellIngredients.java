package com.ourmagic.magic;

import com.ourmagic.item.GrimoireItem;
import com.ourmagic.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class SpellIngredients {
    private SpellIngredients() {
    }

    public static List<Requirement> requirementsFor(String spellKey) {
        Map<String, Requirement> merged = new LinkedHashMap<>();
        for (Requirement requirement : payloadRequirementsFor(spellKey)) {
            merged.merge(requirement.key(), requirement, Requirement::merge);
        }
        for (Requirement requirement : shapeRequirements(SpellRegistry.shapeKey(spellKey))) {
            merged.merge(requirement.key(), requirement, Requirement::merge);
        }
        return List.copyOf(merged.values());
    }

    public static List<Requirement> payloadRequirementsFor(String spellKey) {
        Map<String, Requirement> merged = new LinkedHashMap<>();
        for (String payload : SpellRegistry.payloadParts(spellKey)) {
            for (Requirement requirement : baseRequirements(payload)) {
                merged.merge(requirement.key(), requirement, Requirement::merge);
            }
        }
        return List.copyOf(merged.values());
    }

    public static boolean has(Container inventory, String spellKey) {
        for (Requirement requirement : requirementsFor(spellKey)) {
            if (count(inventory, requirement) < requirement.count()) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasPayloadRequirements(Container inventory, String spellKey) {
        for (Requirement requirement : payloadRequirementsFor(spellKey)) {
            if (count(inventory, requirement) < requirement.count()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isUnlocked(Container inventory, String spellKey) {
        List<String> requestedPayloads = SpellRegistry.payloadParts(spellKey);
        if (requestedPayloads.isEmpty()) {
            return false;
        }
        return has(inventory, spellKey)
                || hasGrimoireSpell(inventory, spellKey)
                || grimoirePayloads(inventory).containsAll(requestedPayloads) && hasShapeRequirements(inventory, spellKey);
    }

    public static boolean hasGrimoireSpell(Container inventory, String spellKey) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.GRIMOIRE.get()) && GrimoireItem.hasMagicCharge(stack) && GrimoireItem.containsSpell(stack, spellKey)) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> grimoirePayloads(Container inventory) {
        Set<String> payloads = new LinkedHashSet<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.GRIMOIRE.get()) && GrimoireItem.hasMagicCharge(stack)) {
                GrimoireItem.spellKeys(stack).forEach(spell -> payloads.addAll(SpellRegistry.payloadParts(spell)));
            }
        }
        return Set.copyOf(payloads);
    }

    public static void consume(Container inventory, String spellKey) {
        for (Requirement requirement : requirementsFor(spellKey)) {
            consume(inventory, requirement);
        }
    }

    public static void consumeShapeRequirements(Container inventory, String spellKey) {
        for (Requirement requirement : shapeRequirements(SpellRegistry.shapeKey(spellKey))) {
            consume(inventory, requirement);
        }
    }

    public static boolean hasShapeRequirements(Container inventory, String spellKey) {
        for (Requirement requirement : shapeRequirements(SpellRegistry.shapeKey(spellKey))) {
            if (count(inventory, requirement) < requirement.count()) {
                return false;
            }
        }
        return true;
    }

    private static void consume(Container inventory, Requirement requirement) {
            int remaining = requirement.count();
            for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty() && requirement.matches(stack)) {
                    int used = Math.min(remaining, stack.getCount());
                    stack.shrink(used);
                    remaining -= used;
                    inventory.setChanged();
                }
            }
    }

    public static int count(Container inventory, Requirement requirement) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && requirement.matches(stack)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static List<Requirement> baseRequirements(String payload) {
        return switch (payload) {
            case "arrow" -> List.of(item("arrow", Items.ARROW, 3), item("feather", Items.FEATHER, 1));
            case "bind" -> List.of(item("lead", Items.LEAD, 1), item("ender_pearl", Items.ENDER_PEARL, 1), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "blast" -> List.of(item("fire_charge", Items.FIRE_CHARGE, 1), item("gunpowder", Items.GUNPOWDER, 2));
            case "blind" -> List.of(item("ink_sac", Items.INK_SAC, 1), item("fermented_spider_eye", Items.FERMENTED_SPIDER_EYE, 1));
            case "blink" -> List.of(item("ender_pearl", Items.ENDER_PEARL, 1), item("amethyst_shard", Items.AMETHYST_SHARD, 2));
            case "bubble" -> List.of(item("prismarine_crystals", Items.PRISMARINE_CRYSTALS, 2), potion("water_potion", Potions.WATER, Items.POTION, 1));
            case "explode" -> List.of(item("tnt", Items.TNT, 1), item("gunpowder", Items.GUNPOWDER, 2));
            case "fire", "fireball" -> List.of(item("blaze_powder", Items.BLAZE_POWDER, 1), item("fire_charge", Items.FIRE_CHARGE, 1));
            case "fire_place" -> List.of(item("flint", Items.FLINT, 1), item("coal", Items.COAL, 1));
            case "frost" -> List.of(item("snowball", Items.SNOWBALL, 4), item("packed_ice", Items.PACKED_ICE, 1));
            case "gather" -> List.of(item("hopper", Items.HOPPER, 1), item("redstone", Items.REDSTONE, 2));
            case "heal" -> List.of(potion("healing_potion", Potions.HEALING, Items.POTION, 1), item("glistering_melon_slice", Items.GLISTERING_MELON_SLICE, 1));
            case "levitate" -> List.of(item("feather", Items.FEATHER, 2), item("phantom_membrane", Items.PHANTOM_MEMBRANE, 1));
            case "lightning" -> List.of(item("lightning_talisman", ModItems.LIGHTNING_TALISMAN.get(), 1));
            case "missile" -> List.of(item("amethyst_shard", Items.AMETHYST_SHARD, 1), item("gunpowder", Items.GUNPOWDER, 1));
            case "nullify" -> List.of(item("milk_bucket", Items.MILK_BUCKET, 1), item("echo_shard", Items.ECHO_SHARD, 1), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "push" -> List.of(item("piston", Items.PISTON, 1), item("slime_ball", Items.SLIME_BALL, 1));
            case "regenerate" -> List.of(potion("regeneration_potion", Potions.REGENERATION, Items.POTION, 1), item("golden_apple", Items.GOLDEN_APPLE, 1));
            case "shield" -> List.of(item("temporary_shield", ModItems.TEMPORARY_SHIELD.get(), 1), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "stun" -> List.of(item("phantom_membrane", Items.PHANTOM_MEMBRANE, 1), item("fermented_spider_eye", Items.FERMENTED_SPIDER_EYE, 1), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "warp" -> List.of(item("ender_pearl", Items.ENDER_PEARL, 2), item("echo_shard", Items.ECHO_SHARD, 1), item("amethyst_shard", Items.AMETHYST_SHARD, 2));
            default -> List.of(item("amethyst_shard", Items.AMETHYST_SHARD, 1));
        };
    }

    private static List<Requirement> shapeRequirements(String shape) {
        return switch (shape) {
            case "self" -> List.of(item("anchor_talisman", ModItems.ANCHOR_TALISMAN.get(), 1));
            case "target" -> List.of(item("seeker_talisman", ModItems.SEEKER_TALISMAN.get(), 1));
            case "point" -> List.of(item("waypoint_talisman", ModItems.WAYPOINT_TALISMAN.get(), 1));
            case "block" -> List.of(item("mason_talisman", ModItems.MASON_TALISMAN.get(), 1));
            case "self_aoe", "self_area" -> List.of(item("pulse_talisman", ModItems.PULSE_TALISMAN.get(), 2));
            case "target_aoe", "target_area" -> List.of(item("stormcall_talisman", ModItems.STORMCALL_TALISMAN.get(), 2));
            case "aoe" -> List.of(item("conflux_talisman", ModItems.CONFLUX_TALISMAN.get(), 2));
            case "ally_self_aoe" -> List.of(item("sanctuary_talisman", ModItems.SANCTUARY_TALISMAN.get(), 2));
            case "ally_target_aoe" -> List.of(item("beacon_talisman", ModItems.BEACON_TALISMAN.get(), 2));
            case "items_self_aoe" -> List.of(item("magnet_talisman", ModItems.MAGNET_TALISMAN.get(), 2));
            case "water_target_aoe" -> List.of(item("tide_talisman", ModItems.TIDE_TALISMAN.get(), 2));
            default -> List.of();
        };
    }

    private static Requirement item(String key, Item item, int count) {
        ItemStack display = new ItemStack(item);
        return new Requirement(key, display, count, stack -> stack.is(item));
    }

    private static Requirement potion(String key, net.minecraft.world.item.alchemy.Potion potion, Item item, int count) {
        ItemStack display = PotionUtils.setPotion(new ItemStack(item), potion);
        return new Requirement(key, display, count, stack -> stack.is(item) && PotionUtils.getPotion(stack) == potion);
    }

    public record Requirement(String key, ItemStack display, int count, Predicate<ItemStack> matcher) {
        private Requirement merge(Requirement other) {
            return new Requirement(key, display.copy(), count + other.count, matcher);
        }

        public boolean matches(ItemStack stack) {
            return matcher.test(stack);
        }

        public Component displayName() {
            return display.getHoverName();
        }
    }
}
