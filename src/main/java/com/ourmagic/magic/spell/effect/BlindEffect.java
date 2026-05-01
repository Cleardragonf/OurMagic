package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;

public class BlindEffect extends ComposedSpellEffect {
    public BlindEffect() {
        this(SpellShapes.lookedLiving(18));
    }

    public BlindEffect(SpellShape shape) {
        super(shape, new BlindPayload(), ParticleTypes.SQUID_INK);
    }
}
