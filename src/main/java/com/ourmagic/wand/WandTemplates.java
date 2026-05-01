package com.ourmagic.wand;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class WandTemplates {
    public static final String DEFAULT_TEMPLATE = "wand";
    public static final String ADMIN_TEMPLATE = "admin_wand";
    private static final Map<String, WandTemplate> TEMPLATES = new LinkedHashMap<>();

    static {
        register(new WandTemplate(DEFAULT_TEMPLATE, "Wand", 1.0f, List.of(rolledSpell(SpellRegistry.get("missile@self"), 1.0F, 1.0F))));
        register(new WandTemplate(ADMIN_TEMPLATE, "Admin Wand", 2.0f, adminSpells()));
    }

    private WandTemplates() {
    }

    private static void register(WandTemplate template) {
        TEMPLATES.put(template.key(), template);
    }

    public static Optional<WandTemplate> get(String key) {
        return Optional.ofNullable(TEMPLATES.get(key));
    }

    public static Set<String> keys() {
        return TEMPLATES.keySet();
    }

    public static ItemStack apply(ItemStack stack, String key) {
        WandTemplate template = get(key).orElse(TEMPLATES.get(DEFAULT_TEMPLATE));
        WandData.fromTemplate(template).save(stack);
        return stack;
    }

    public static ItemStack applyRandom(ItemStack stack, RandomSource random) {
        int spellCount = 1;
        if (random.nextFloat() < 0.20F) {
            spellCount++;
        }
        if (random.nextFloat() < 0.05F) {
            spellCount++;
        }

        List<WandData.WandSpellData> spells = new ArrayList<>();
        for (int i = 0; i < spellCount; i++) {
            Spell spell = SpellRegistry.randomSpell(random);
            if (spell == null) {
                continue;
            }
            int cost = ranged(random, spell.manaCost(), 0.70F, 1.35F);
            int cooldown = ranged(random, spell.cooldownTicks(), 0.70F, 1.40F);
            spells.add(new WandData.WandSpellData(spell.key(), cost, cooldown));
        }
        if (spells.isEmpty()) {
            spells.add(rolledSpell(SpellRegistry.get("missile@self"), 1.0F, 1.0F));
        }

        new WandData(DEFAULT_TEMPLATE, "Wand", 0.85F + random.nextFloat() * 0.45F, spells, 0, 0).save(stack);
        return stack;
    }

    public static ItemStack applyAdmin(ItemStack stack) {
        WandData.fromTemplate(TEMPLATES.get(ADMIN_TEMPLATE)).save(stack);
        return stack;
    }

    public static void ensureInitialized(ItemStack stack, boolean admin) {
        if (!stack.getOrCreateTag().contains(WandData.TAG_TEMPLATE)) {
            if (admin) {
                applyAdmin(stack);
            } else {
                applyRandom(stack, RandomSource.create());
            }
        }
    }

    public static void ensureInitialized(ItemStack stack) {
        ensureInitialized(stack, false);
    }

    private static int ranged(RandomSource random, int base, float minMultiplier, float maxMultiplier) {
        int min = Math.max(0, Math.round(base * minMultiplier));
        int max = Math.max(min, Math.round(base * maxMultiplier));
        return min + random.nextInt(max - min + 1);
    }

    private static WandData.WandSpellData rolledSpell(Spell spell, float costMultiplier, float cooldownMultiplier) {
        return new WandData.WandSpellData(spell.key(), Math.round(spell.manaCost() * costMultiplier), Math.round(spell.cooldownTicks() * cooldownMultiplier));
    }

    private static List<WandData.WandSpellData> adminSpells() {
        List<WandData.WandSpellData> spells = new ArrayList<>();
        for (Spell spell : SpellRegistry.all()) {
            spells.add(new WandData.WandSpellData(spell.key(), 0, 5));
        }
        return spells;
    }
}
