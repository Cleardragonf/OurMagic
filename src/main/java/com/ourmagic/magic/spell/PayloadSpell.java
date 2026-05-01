package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.ComposedSpellEffect;
import com.ourmagic.magic.spell.effect.PayloadEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import net.minecraft.core.particles.ParticleOptions;

public class PayloadSpell extends BaseSpell {
    public PayloadSpell(String key, int manaCost, int cooldownTicks, SpellShape shape, PayloadEffect payload, String... supportedUpgrades) {
        super(key, manaCost, cooldownTicks, new ComposedSpellEffect(shape, payload), supportedUpgrades);
    }

    public PayloadSpell(String key, int manaCost, int cooldownTicks, SpellShape shape, PayloadEffect payload, ParticleOptions chainParticle, String... supportedUpgrades) {
        super(key, manaCost, cooldownTicks, new ComposedSpellEffect(shape, payload, chainParticle), supportedUpgrades);
    }
}
