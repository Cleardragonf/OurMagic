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
                || hasKnownSpell(inventory, spellKey)
                || knownPayloads(inventory).containsAll(requestedPayloads) && hasShapeRequirements(inventory, spellKey);
    }

    public static boolean hasKnownSpell(Container inventory, String spellKey) {
        return hasGrimoireSpell(inventory, spellKey) || hasArcaneKnowledgeSpell(inventory, spellKey);
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

    public static boolean hasArcaneKnowledgeSpell(Container inventory, String spellKey) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (ArcaneKnowledgeBook.containsSpell(stack, spellKey)) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> knownPayloads(Container inventory) {
        Set<String> payloads = new LinkedHashSet<>();
        payloads.addAll(grimoirePayloads(inventory));
        payloads.addAll(arcaneKnowledgePayloads(inventory));
        return Set.copyOf(payloads);
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

    public static Set<String> arcaneKnowledgePayloads(Container inventory) {
        Set<String> payloads = new LinkedHashSet<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (ArcaneKnowledgeBook.isKnowledgeBook(stack)) {
                ArcaneKnowledgeBook.spellKeys(stack).forEach(spell -> payloads.addAll(SpellRegistry.payloadParts(spell)));
            }
        }
        return Set.copyOf(payloads);
    }

    public static boolean hasArcaneKnowledgePayload(Container inventory, String payload) {
        return arcaneKnowledgePayloads(inventory).contains(payload);
    }

    public static boolean hasArcaneKnowledgeShape(Container inventory, String shape) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!ArcaneKnowledgeBook.isKnowledgeBook(stack)) {
                continue;
            }
            for (String spellKey : ArcaneKnowledgeBook.spellKeys(stack)) {
                if (SpellRegistry.shapeKey(spellKey).equals(shape)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasAnyPayloadRequirement(Container inventory, String payload) {
        for (Requirement requirement : payloadRequirementsFor(payload + "@self")) {
            if (count(inventory, requirement) > 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean consumeExactKnowledge(Container inventory, String spellKey) {
        return consumeExactGrimoireKnowledge(inventory, spellKey)
                || consumeExactArcaneKnowledge(inventory, spellKey);
    }

    public static boolean consumePayloadKnowledge(Container inventory, String spellKey) {
        Set<String> remaining = new LinkedHashSet<>(SpellRegistry.payloadParts(spellKey));
        if (remaining.isEmpty()) {
            return false;
        }

        List<KnowledgeSource> sources = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            Set<String> stackPayloads = new LinkedHashSet<>();
            boolean grimoire = stack.is(ModItems.GRIMOIRE.get()) && GrimoireItem.hasMagicCharge(stack);
            boolean arcaneKnowledge = ArcaneKnowledgeBook.isKnowledgeBook(stack);
            if (grimoire) {
                GrimoireItem.spellKeys(stack).forEach(spell -> stackPayloads.addAll(SpellRegistry.payloadParts(spell)));
            } else if (arcaneKnowledge) {
                ArcaneKnowledgeBook.spellKeys(stack).forEach(spell -> stackPayloads.addAll(SpellRegistry.payloadParts(spell)));
            } else {
                continue;
            }

            if (remaining.removeIf(stackPayloads::contains)) {
                sources.add(new KnowledgeSource(stack, grimoire));
            }
        }
        if (!remaining.isEmpty()) {
            return false;
        }

        for (KnowledgeSource source : sources) {
            if (source.grimoire()) {
                GrimoireItem.consumeMagicCharge(source.stack());
            } else {
                source.stack().shrink(1);
            }
        }
        inventory.setChanged();
        return true;
    }

    private static boolean consumeExactGrimoireKnowledge(Container inventory, String spellKey) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.GRIMOIRE.get()) && GrimoireItem.containsSpell(stack, spellKey) && GrimoireItem.consumeMagicCharge(stack)) {
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    private static boolean consumeExactArcaneKnowledge(Container inventory, String spellKey) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (ArcaneKnowledgeBook.containsSpell(stack, spellKey)) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    private static boolean consumePayloadGrimoireKnowledge(Container inventory, String spellKey) {
        Set<String> remaining = new LinkedHashSet<>(SpellRegistry.payloadParts(spellKey));
        if (remaining.isEmpty()) {
            return false;
        }

        List<ItemStack> grimoires = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(ModItems.GRIMOIRE.get()) || !GrimoireItem.hasMagicCharge(stack)) {
                continue;
            }
            Set<String> stackPayloads = new LinkedHashSet<>();
            GrimoireItem.spellKeys(stack).forEach(spell -> stackPayloads.addAll(SpellRegistry.payloadParts(spell)));
            if (remaining.removeIf(stackPayloads::contains)) {
                grimoires.add(stack);
            }
        }
        if (!remaining.isEmpty()) {
            return false;
        }
        for (ItemStack grimoire : grimoires) {
            GrimoireItem.consumeMagicCharge(grimoire);
        }
        inventory.setChanged();
        return true;
    }

    private static boolean consumePayloadArcaneKnowledge(Container inventory, String spellKey) {
        Set<String> remaining = new LinkedHashSet<>(SpellRegistry.payloadParts(spellKey));
        if (remaining.isEmpty()) {
            return false;
        }

        List<ItemStack> books = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!ArcaneKnowledgeBook.isKnowledgeBook(stack)) {
                continue;
            }
            Set<String> stackPayloads = new LinkedHashSet<>();
            ArcaneKnowledgeBook.spellKeys(stack).forEach(spell -> stackPayloads.addAll(SpellRegistry.payloadParts(spell)));
            if (remaining.removeIf(stackPayloads::contains)) {
                books.add(stack);
            }
        }
        if (!remaining.isEmpty()) {
            return false;
        }
        books.forEach(stack -> stack.shrink(1));
        inventory.setChanged();
        return true;
    }

    private record KnowledgeSource(ItemStack stack, boolean grimoire) {
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

    public static boolean hasAnyShapeRequirement(Container inventory, String shape) {
        for (Requirement requirement : shapeRequirements(shape)) {
            if (count(inventory, requirement) > 0) {
                return true;
            }
        }
        return false;
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
            case "anchor" -> List.of(item("chain", Items.CHAIN, 2), item("lodestone", Items.LODESTONE, 1));
            case "anti_explosion" -> List.of(item("obsidian", Items.OBSIDIAN, 2), item("gunpowder", Items.GUNPOWDER, 2), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "anti_fire" -> List.of(potion("fire_resistance_potion", Potions.FIRE_RESISTANCE, Items.POTION, 1), item("blue_ice", Items.BLUE_ICE, 1));
            case "anti_grief" -> List.of(item("reinforced_deepslate", Items.REINFORCED_DEEPSLATE, 1), item("chain", Items.CHAIN, 2));
            case "anti_magic" -> List.of(item("echo_shard", Items.ECHO_SHARD, 1), item("amethyst_shard", Items.AMETHYST_SHARD, 2), item("crying_obsidian", Items.CRYING_OBSIDIAN, 1));
            case "anti_projectile" -> List.of(item("shield", Items.SHIELD, 1), item("arrow", Items.ARROW, 4));
            case "anti_teleport" -> List.of(item("ender_pearl", Items.ENDER_PEARL, 2), item("chain", Items.CHAIN, 2));
            case "anti_decay" -> List.of(item("moss_block", Items.MOSS_BLOCK, 2), item("bone_meal", Items.BONE_MEAL, 4));
            case "anti_summon" -> List.of(item("soul_lantern", Items.SOUL_LANTERN, 1), item("bone", Items.BONE, 4));
            case "anti_water" -> List.of(item("sponge", Items.SPONGE, 1), item("prismarine_crystals", Items.PRISMARINE_CRYSTALS, 2));
            case "air" -> List.of(item("glass_bottle", Items.GLASS_BOTTLE, 3), item("feather", Items.FEATHER, 2));
            case "air_bubble" -> List.of(item("sponge", Items.SPONGE, 2), item("glass", Items.GLASS, 4), item("prismarine_crystals", Items.PRISMARINE_CRYSTALS, 2));
            case "agility" -> List.of(potion("swiftness_potion", Potions.SWIFTNESS, Items.POTION, 1), item("rabbit_foot", Items.RABBIT_FOOT, 1), item("feather", Items.FEATHER, 2));
            case "alarm" -> List.of(item("note_block", Items.NOTE_BLOCK, 1), item("redstone", Items.REDSTONE, 2));
            case "arrow" -> List.of(item("arrow", Items.ARROW, 3), item("feather", Items.FEATHER, 1));
            case "bind" -> List.of(item("lead", Items.LEAD, 1), item("ender_pearl", Items.ENDER_PEARL, 1), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "blast" -> List.of(item("fire_charge", Items.FIRE_CHARGE, 1), item("gunpowder", Items.GUNPOWDER, 2));
            case "blastguard" -> List.of(item("obsidian", Items.OBSIDIAN, 2), item("shield", Items.SHIELD, 1), item("gunpowder", Items.GUNPOWDER, 2));
            case "blind" -> List.of(item("ink_sac", Items.INK_SAC, 1), item("fermented_spider_eye", Items.FERMENTED_SPIDER_EYE, 1));
            case "blink" -> List.of(item("ender_pearl", Items.ENDER_PEARL, 1), item("amethyst_shard", Items.AMETHYST_SHARD, 2));
            case "bubble" -> List.of(item("prismarine_crystals", Items.PRISMARINE_CRYSTALS, 2), potion("water_potion", Potions.WATER, Items.POTION, 1));
            case "camouflage" -> List.of(item("moss_block", Items.MOSS_BLOCK, 2), item("gray_dye", Items.GRAY_DYE, 2), item("glass", Items.GLASS, 2));
            case "charm" -> List.of(item("poppy", Items.POPPY, 2), item("honey_bottle", Items.HONEY_BOTTLE, 1));
            case "cleanse" -> List.of(item("milk_bucket", Items.MILK_BUCKET, 1), item("honey_bottle", Items.HONEY_BOTTLE, 1));
            case "conjure" -> List.of(item("soul_lantern", Items.SOUL_LANTERN, 1), item("amethyst_shard", Items.AMETHYST_SHARD, 2));
            case "curse" -> List.of(item("fermented_spider_eye", Items.FERMENTED_SPIDER_EYE, 1), item("wither_rose", Items.WITHER_ROSE, 1));
            case "dark" -> List.of(item("black_dye", Items.BLACK_DYE, 2), item("sculk", Items.SCULK, 1));
            case "disarm" -> List.of(item("tripwire_hook", Items.TRIPWIRE_HOOK, 1), item("iron_nugget", Items.IRON_NUGGET, 4));
            case "dispel" -> List.of(item("echo_shard", Items.ECHO_SHARD, 1), item("milk_bucket", Items.MILK_BUCKET, 1));
            case "entry_filter" -> List.of(item("iron_door", Items.IRON_DOOR, 1), item("ender_pearl", Items.ENDER_PEARL, 1));
            case "echo" -> List.of(item("echo_shard", Items.ECHO_SHARD, 1), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "explode" -> List.of(item("tnt", Items.TNT, 1), item("gunpowder", Items.GUNPOWDER, 2));
            case "fallguard" -> List.of(item("feather", Items.FEATHER, 4), item("phantom_membrane", Items.PHANTOM_MEMBRANE, 1), item("slime_ball", Items.SLIME_BALL, 1));
            case "fire", "fireball" -> List.of(item("blaze_powder", Items.BLAZE_POWDER, 1), item("fire_charge", Items.FIRE_CHARGE, 1));
            case "fireguard" -> List.of(potion("fire_resistance_potion", Potions.FIRE_RESISTANCE, Items.POTION, 1), item("magma_cream", Items.MAGMA_CREAM, 1));
            case "fire_place" -> List.of(item("flint", Items.FLINT, 1), item("coal", Items.COAL, 1));
            case "fertility" -> List.of(item("bone_meal", Items.BONE_MEAL, 8), item("moss_block", Items.MOSS_BLOCK, 1));
            case "fortify" -> List.of(item("shield", Items.SHIELD, 1), item("iron_ingot", Items.IRON_INGOT, 2), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "freeze" -> List.of(item("packed_ice", Items.PACKED_ICE, 2), item("snowball", Items.SNOWBALL, 4));
            case "frost" -> List.of(item("snowball", Items.SNOWBALL, 4), item("packed_ice", Items.PACKED_ICE, 1));
            case "gather" -> List.of(item("hopper", Items.HOPPER, 1), item("redstone", Items.REDSTONE, 2));
            case "gravity" -> List.of(item("ender_pearl", Items.ENDER_PEARL, 1), item("lodestone", Items.LODESTONE, 1));
            case "grow" -> List.of(item("bone_meal", Items.BONE_MEAL, 12), item("golden_carrot", Items.GOLDEN_CARROT, 1));
            case "hex" -> List.of(item("spider_eye", Items.SPIDER_EYE, 1), item("glow_ink_sac", Items.GLOW_INK_SAC, 1));
            case "heal" -> List.of(potion("healing_potion", Potions.HEALING, Items.POTION, 1), item("glistering_melon_slice", Items.GLISTERING_MELON_SLICE, 1));
            case "illusion" -> List.of(item("glass", Items.GLASS, 2), item("phantom_membrane", Items.PHANTOM_MEMBRANE, 1));
            case "levitate" -> List.of(item("feather", Items.FEATHER, 2), item("phantom_membrane", Items.PHANTOM_MEMBRANE, 1));
            case "life_ward" -> List.of(item("totem_of_undying", Items.TOTEM_OF_UNDYING, 1), item("ghast_tear", Items.GHAST_TEAR, 1), item("golden_apple", Items.GOLDEN_APPLE, 1));
            case "lifedrain" -> List.of(item("ghast_tear", Items.GHAST_TEAR, 1), item("fermented_spider_eye", Items.FERMENTED_SPIDER_EYE, 1));
            case "light" -> List.of(item("glowstone_dust", Items.GLOWSTONE_DUST, 4), item("torch", Items.TORCH, 8));
            case "lightning" -> List.of(item("lightning_talisman", ModItems.LIGHTNING_TALISMAN.get(), 1));
            case "lockdown" -> List.of(item("iron_bars", Items.IRON_BARS, 4), item("ender_pearl", Items.ENDER_PEARL, 1), item("chain", Items.CHAIN, 2));
            case "item_guard" -> List.of(item("hopper", Items.HOPPER, 1), item("chest", Items.CHEST, 1));
            case "storage_lock" -> List.of(item("chest", Items.CHEST, 1), item("iron_ingot", Items.IRON_INGOT, 2));
            case "mana_drain" -> List.of(item("soul_sand", Items.SOUL_SAND, 2), item("amethyst_shard", Items.AMETHYST_SHARD, 2));
            case "mana_shield" -> List.of(item("shield", Items.SHIELD, 1), item("lapis_lazuli", Items.LAPIS_LAZULI, 4), item("amethyst_shard", Items.AMETHYST_SHARD, 2));
            case "manaburn" -> List.of(item("soul_sand", Items.SOUL_SAND, 1), item("blaze_powder", Items.BLAZE_POWDER, 1));
            case "missile" -> List.of(item("amethyst_shard", Items.AMETHYST_SHARD, 1), item("gunpowder", Items.GUNPOWDER, 1));
            case "reflect" -> List.of(item("glass_pane", Items.GLASS_PANE, 4), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "reflect_projectile" -> List.of(item("shield", Items.SHIELD, 1), item("glass_pane", Items.GLASS_PANE, 4), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "nullify" -> List.of(item("milk_bucket", Items.MILK_BUCKET, 1), item("echo_shard", Items.ECHO_SHARD, 1), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "overload" -> List.of(item("redstone_block", Items.REDSTONE_BLOCK, 1), item("glowstone_dust", Items.GLOWSTONE_DUST, 2));
            case "phase" -> List.of(item("phantom_membrane", Items.PHANTOM_MEMBRANE, 2), item("ender_pearl", Items.ENDER_PEARL, 1));
            case "push" -> List.of(item("piston", Items.PISTON, 1), item("slime_ball", Items.SLIME_BALL, 1));
            case "recall" -> List.of(item("lodestone", Items.LODESTONE, 1), item("ender_pearl", Items.ENDER_PEARL, 2), item("echo_shard", Items.ECHO_SHARD, 1));
            case "regenerate" -> List.of(potion("regeneration_potion", Potions.REGENERATION, Items.POTION, 1), item("golden_apple", Items.GOLDEN_APPLE, 1));
            case "reveal" -> List.of(item("glow_ink_sac", Items.GLOW_INK_SAC, 1), item("spyglass", Items.SPYGLASS, 1));
            case "rune" -> List.of(item("chiseled_stone_bricks", Items.CHISELED_STONE_BRICKS, 1), item("redstone", Items.REDSTONE, 2), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "sanctuary" -> List.of(item("totem_of_undying", Items.TOTEM_OF_UNDYING, 1), item("golden_apple", Items.GOLDEN_APPLE, 1));
            case "scry" -> List.of(item("spyglass", Items.SPYGLASS, 1), item("glow_ink_sac", Items.GLOW_INK_SAC, 1));
            case "shield" -> List.of(item("temporary_shield", ModItems.TEMPORARY_SHIELD.get(), 1), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "silence" -> List.of(item("sculk_sensor", Items.SCULK_SENSOR, 1), item("ink_sac", Items.INK_SAC, 1));
            case "stasis" -> List.of(item("packed_ice", Items.PACKED_ICE, 2), item("clock", Items.CLOCK, 1));
            case "stun" -> List.of(item("phantom_membrane", Items.PHANTOM_MEMBRANE, 1), item("fermented_spider_eye", Items.FERMENTED_SPIDER_EYE, 1), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "summon", "summon_random" -> List.of(item("amethyst_shard", Items.AMETHYST_SHARD, 3), item("soul_sand", Items.SOUL_SAND, 1));
            case "summon_undead" -> List.of(item("bone", Items.BONE, 4), item("rotten_flesh", Items.ROTTEN_FLESH, 2), item("soul_sand", Items.SOUL_SAND, 1));
            case "summon_beast" -> List.of(item("bone", Items.BONE, 2), item("leather", Items.LEATHER, 2), item("sweet_berries", Items.SWEET_BERRIES, 2));
            case "summon_guardian" -> List.of(item("iron_block", Items.IRON_BLOCK, 1), item("snow_block", Items.SNOW_BLOCK, 2), item("amethyst_shard", Items.AMETHYST_SHARD, 2));
            case "summon_arcane" -> List.of(item("blaze_powder", Items.BLAZE_POWDER, 2), item("redstone", Items.REDSTONE, 2), item("amethyst_shard", Items.AMETHYST_SHARD, 2));
            case "summon_swarm" -> List.of(item("spider_eye", Items.SPIDER_EYE, 2), item("gunpowder", Items.GUNPOWDER, 1), item("amethyst_shard", Items.AMETHYST_SHARD, 1));
            case "temporal" -> List.of(item("clock", Items.CLOCK, 1), item("echo_shard", Items.ECHO_SHARD, 1));
            case "thaw" -> List.of(item("magma_block", Items.MAGMA_BLOCK, 1), item("torch", Items.TORCH, 4));
            case "transmute" -> List.of(item("copper_ingot", Items.COPPER_INGOT, 2), item("moss_block", Items.MOSS_BLOCK, 1));
            case "ward" -> List.of(item("amethyst_shard", Items.AMETHYST_SHARD, 2), item("shield", Items.SHIELD, 1));
            case "weakening" -> List.of(item("fermented_spider_eye", Items.FERMENTED_SPIDER_EYE, 1), item("bone", Items.BONE, 2));
            case "weather" -> List.of(item("lightning_rod", Items.LIGHTNING_ROD, 1), item("glass", Items.GLASS, 4));
            case "warp" -> List.of(item("ender_pearl", Items.ENDER_PEARL, 2), item("echo_shard", Items.ECHO_SHARD, 1), item("amethyst_shard", Items.AMETHYST_SHARD, 2));
            default -> List.of(item("amethyst_shard", Items.AMETHYST_SHARD, 1));
        };
    }

    private static List<Requirement> shapeRequirements(String shape) {
        if (shape.startsWith("ward_player_")
                || shape.startsWith("ward_mob_")
                || shape.startsWith("ward_entity_")
                || shape.startsWith("ward_entity_type_")) {
            return List.of(item("pulse_talisman", ModItems.PULSE_TALISMAN.get(), 2));
        }
        return switch (shape) {
            case "self" -> List.of(item("anchor_talisman", ModItems.ANCHOR_TALISMAN.get(), 1));
            case "target" -> List.of(item("seeker_talisman", ModItems.SEEKER_TALISMAN.get(), 1));
            case "point" -> List.of(item("waypoint_talisman", ModItems.WAYPOINT_TALISMAN.get(), 1));
            case "block" -> List.of(item("mason_talisman", ModItems.MASON_TALISMAN.get(), 1));
            case "target_aoe" -> List.of(item("stormcall_talisman", ModItems.STORMCALL_TALISMAN.get(), 2));
            case "aoe" -> List.of(item("conflux_talisman", ModItems.CONFLUX_TALISMAN.get(), 2));
            case "ally_aoe" -> List.of(item("sanctuary_talisman", ModItems.SANCTUARY_TALISMAN.get(), 2));
            case "ally_target_aoe" -> List.of(item("beacon_talisman", ModItems.BEACON_TALISMAN.get(), 2));
            case "items_self_aoe" -> List.of(item("magnet_talisman", ModItems.MAGNET_TALISMAN.get(), 2));
            case "water_target_aoe" -> List.of(item("tide_talisman", ModItems.TIDE_TALISMAN.get(), 2));
            case "ward_non_allied", "ward_hostile", "ward_players", "ward_allies", "ward_mobs", "ward_monsters", "ward_passive", "ward_animals", "ward_any" -> List.of(item("pulse_talisman", ModItems.PULSE_TALISMAN.get(), 2));
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
