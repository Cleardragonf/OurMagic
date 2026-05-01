package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;

public class HealEffect extends ComposedSpellEffect {
    public HealEffect() {
        this(SpellShapes.playersAroundSelf(4, ParticleTypes.HAPPY_VILLAGER));
    }

    public HealEffect(SpellShape shape) {
        super(shape, new HealPayload(6.0F, 4.0F, 8));
    }
}
