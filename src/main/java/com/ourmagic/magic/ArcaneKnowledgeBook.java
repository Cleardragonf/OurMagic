package com.ourmagic.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ArcaneKnowledgeBook {
    private static final String TAG_ARCANE_KNOWLEDGE = "OurMagicArcaneKnowledge";

    private ArcaneKnowledgeBook() {
    }

    public static ItemStack create(RandomSource random, boolean strong) {
        ItemStack stack = new ItemStack(Items.BOOK);
        List<String> spells = randomSpells(random, spellCount(random, strong));
        writeSpells(stack, spells);
        stack.setHoverName(Component.literal("Book of Arcane Knowledge").withStyle(ChatFormatting.LIGHT_PURPLE));
        return stack;
    }

    public static boolean isKnowledgeBook(ItemStack stack) {
        return stack.is(Items.BOOK) && stack.hasTag() && !spellKeys(stack).isEmpty();
    }

    public static boolean containsSpell(ItemStack stack, String spellKey) {
        return spellKeys(stack).contains(spellKey);
    }

    public static List<String> spellKeys(ItemStack stack) {
        if (!stack.is(Items.BOOK) || !stack.hasTag()) {
            return List.of();
        }

        ListTag tags = stack.getOrCreateTag().getList(TAG_ARCANE_KNOWLEDGE, 8);
        List<String> spells = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++) {
            String spell = tags.getString(i);
            if (SpellRegistry.get(spell) != null) {
                spells.add(spell);
            }
        }
        return List.copyOf(spells);
    }

    private static void writeSpells(ItemStack stack, List<String> spells) {
        ListTag tags = new ListTag();
        for (String spell : spells) {
            tags.add(StringTag.valueOf(spell));
        }
        stack.getOrCreateTag().put(TAG_ARCANE_KNOWLEDGE, tags);
    }

    private static int spellCount(RandomSource random, boolean strong) {
        if (strong) {
            float roll = random.nextFloat();
            if (roll < 0.15F) {
                return 4;
            }
            if (roll < 0.50F) {
                return 3;
            }
            return 2;
        }

        float roll = random.nextFloat();
        if (roll < 0.01F) {
            return 3;
        }
        if (roll < 0.11F) {
            return 2;
        }
        return 1;
    }

    private static List<String> randomSpells(RandomSource random, int count) {
        Set<String> spells = new LinkedHashSet<>();
        int attempts = 0;
        while (spells.size() < count && attempts++ < count * 64) {
            Spell spell = SpellRegistry.randomArcaneKnowledgeSpell(random);
            if (spell != null) {
                spells.add(spell.key());
            }
        }
        List<String> fallbackRecipes = new ArrayList<>(SpellRegistry.arcaneKnowledgeRecipes());
        while (spells.size() < count && !fallbackRecipes.isEmpty()) {
            spells.add(fallbackRecipes.remove(random.nextInt(fallbackRecipes.size())));
        }
        if (spells.isEmpty()) {
            spells.add("arrow@target");
        }
        return List.copyOf(spells);
    }
}
