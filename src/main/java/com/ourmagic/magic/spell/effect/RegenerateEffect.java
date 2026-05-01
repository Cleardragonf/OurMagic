package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;

public class RegenerateEffect extends ComposedSpellEffect {
    public RegenerateEffect() {
        this(SpellShapes.playersAroundSelf(4, ParticleTypes.HAPPY_VILLAGER));
    }

    public RegenerateEffect(SpellShape shape) {
        super(shape, new RegeneratePayload());
    }
}
