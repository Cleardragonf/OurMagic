package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;

public class LightningEffect extends ComposedSpellEffect {
    public LightningEffect() {
        this(SpellShapes.lookedLivingOrPoint(30, 20));
    }

    public LightningEffect(SpellShape shape) {
        super(shape, new LightningPayload(), ParticleTypes.ELECTRIC_SPARK);
    }
}
