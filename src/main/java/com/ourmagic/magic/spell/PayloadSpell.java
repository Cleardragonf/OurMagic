package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.runtime.ComposedSpellEffect;
import com.ourmagic.magic.spell.payloads.PayloadEffect;
import com.ourmagic.magic.spell.shapes.SpellShape;
import net.minecraft.core.particles.ParticleOptions;

public class PayloadSpell extends BaseSpell {
    public PayloadSpell(String key, int manaCost, int cooldownTicks, SpellShape shape, PayloadEffect payload, String... supportedUpgrades) {
        super(key, manaCost, cooldownTicks, new ComposedSpellEffect(shape, payload), supportedUpgrades);
    }

    public PayloadSpell(String key, int manaCost, int cooldownTicks, SpellShape shape, PayloadEffect payload, ParticleOptions chainParticle, String... supportedUpgrades) {
        super(key, manaCost, cooldownTicks, new ComposedSpellEffect(shape, payload, chainParticle), supportedUpgrades);
    }

    public PayloadSpell(String key, int minManaCost, int maxManaCost, int minCooldownTicks, int maxCooldownTicks, SpellShape shape, PayloadEffect payload, ParticleOptions chainParticle, String... supportedUpgrades) {
        super(key, minManaCost, maxManaCost, minCooldownTicks, maxCooldownTicks, new ComposedSpellEffect(shape, payload, chainParticle), supportedUpgrades);
    }
}
