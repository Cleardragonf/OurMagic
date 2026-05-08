package com.ourmagic.wand;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellInstance;
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
        register(new WandTemplate(DEFAULT_TEMPLATE, "Wand", 1.0f, List.of(fixedSpell(SpellRegistry.get("missile@self")))));
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
            spells.add(rolledSpell(spell, random));
        }
        if (spells.isEmpty()) {
            spells.add(fixedSpell(SpellRegistry.get("missile@self")));
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

    private static WandData.WandSpellData rolledSpell(Spell spell, RandomSource random) {
        SpellInstance instance = SpellInstance.roll(spell.key(), random);
        return new WandData.WandSpellData(instance.key(), instance.displayName(), instance.manaCost(), instance.cooldownTicks());
    }

    private static WandData.WandSpellData fixedSpell(Spell spell) {
        return new WandData.WandSpellData(spell.key(), spell.manaCost(), spell.cooldownTicks());
    }

    private static List<WandData.WandSpellData> adminSpells() {
        List<WandData.WandSpellData> spells = new ArrayList<>();
        for (Spell spell : SpellRegistry.all()) {
            spells.add(new WandData.WandSpellData(spell.key(), 0, 5));
        }
        return spells;
    }
}
