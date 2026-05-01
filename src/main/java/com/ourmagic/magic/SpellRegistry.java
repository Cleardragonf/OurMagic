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
import com.ourmagic.magic.spell.PayloadSpell;
import com.ourmagic.magic.spell.PushSpell;
import com.ourmagic.magic.spell.RegenerateSpell;
import com.ourmagic.magic.spell.effect.BlindPayload;
import com.ourmagic.magic.spell.effect.CompositePayload;
import com.ourmagic.magic.spell.effect.ExplosionPayload;
import com.ourmagic.magic.spell.effect.LightningPayload;
import com.ourmagic.magic.spell.effect.SpellShapes;
import net.minecraft.core.particles.ParticleTypes;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SpellRegistry {
    private static final Map<String, Spell> SPELLS = new LinkedHashMap<>();

    static {
        register(new MissileSpell());
        register(new MissileSpell("missile_target", SpellShapes.lookedLivingOrPoint(24, 16)));
        register(new MissileSpell("missile_area", SpellShapes.livingAroundLookedPoint(24, 16, 4, 4, ParticleTypes.CRIT)));
        register(new PushSpell());
        register(new ArrowSpell());
        register(new BlastSpell());
        register(new FrostSpell());
        register(new BubbleSpell());
        register(new FireballSpell());
        register(new BlindSpell());
        register(new HealSpell());
        register(new HealSpell("heal_target", SpellShapes.lookedLivingOrSelf(18)));
        register(new HealSpell("heal_area", SpellShapes.playersAroundLookedLivingOrSelf(18, 4, ParticleTypes.HAPPY_VILLAGER)));
        register(new BlinkSpell());
        register(new LevitateSpell());
        register(new LightningSpell());
        register(new PayloadSpell("explode", 30, 70, SpellShapes.lookedLivingOrPoint(24, 16), new ExplosionPayload(), Spell.UPGRADE_MULTISTRIKE));
        register(new PayloadSpell("lightning_explode", 55, 110, SpellShapes.lookedLivingOrPoint(30, 20), new CompositePayload(new LightningPayload(), new ExplosionPayload()), ParticleTypes.ELECTRIC_SPARK, Spell.UPGRADE_CHAINING, Spell.UPGRADE_MULTISTRIKE));
        register(new PayloadSpell("blind_explode", 40, 80, SpellShapes.lookedLivingOrPoint(18, 18), new CompositePayload(new BlindPayload(), new ExplosionPayload()), ParticleTypes.SQUID_INK, Spell.UPGRADE_DURATION, Spell.UPGRADE_CHAINING));
        register(new PayloadSpell("lightning_blind_explode", 70, 130, SpellShapes.lookedLivingOrPoint(30, 20), new CompositePayload(new LightningPayload(), new BlindPayload(), new ExplosionPayload()), ParticleTypes.ELECTRIC_SPARK, Spell.UPGRADE_DURATION, Spell.UPGRADE_CHAINING, Spell.UPGRADE_MULTISTRIKE));
        register(new FireSpell());
        register(new GatherSpell());
        register(new RegenerateSpell());
        register(new RegenerateSpell("regenerate_target", SpellShapes.lookedLivingOrSelf(18)));
        register(new RegenerateSpell("regenerate_area", SpellShapes.playersAroundLookedLivingOrSelf(18, 4, ParticleTypes.HAPPY_VILLAGER)));
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
