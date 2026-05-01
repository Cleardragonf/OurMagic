package com.ourmagic.magic;

import com.ourmagic.magic.spell.ArrowSpell;
import com.ourmagic.magic.spell.BlastSpell;
import com.ourmagic.magic.spell.BlinkSpell;
import com.ourmagic.magic.spell.BlindSpell;
import com.ourmagic.magic.spell.BubbleSpell;
import com.ourmagic.magic.spell.FireSpell;
import com.ourmagic.magic.spell.FireballSpell;
import com.ourmagic.magic.spell.FrostSpell;
import com.ourmagic.magic.spell.GatherSpell;
import com.ourmagic.magic.spell.HealSpell;
import com.ourmagic.magic.spell.LevitateSpell;
import com.ourmagic.magic.spell.LightningSpell;
import com.ourmagic.magic.spell.MissileSpell;
import com.ourmagic.magic.spell.PushSpell;
import com.ourmagic.magic.spell.RegenerateSpell;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SpellRegistry {
    private static final Map<String, Spell> SPELLS = new LinkedHashMap<>();

    static {
        register(new MissileSpell());
        register(new PushSpell());
        register(new ArrowSpell());
        register(new BlastSpell());
        register(new FrostSpell());
        register(new BubbleSpell());
        register(new FireballSpell());
        register(new BlindSpell());
        register(new HealSpell());
        register(new BlinkSpell());
        register(new LevitateSpell());
        register(new LightningSpell());
        register(new FireSpell());
        register(new GatherSpell());
        register(new RegenerateSpell());
    }

    private SpellRegistry() {
    }

    private static void register(Spell spell) {
        SPELLS.put(spell.key(), spell);
    }

    public static Spell get(String key) {
        return SPELLS.get(key);
    }

    public static Collection<Spell> all() {
        return SPELLS.values();
    }
}
