package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;

public class FrostEffect extends ComposedSpellEffect {
    public FrostEffect() {
        this(SpellShapes.waterBlocksAroundLookedLivingOrPoint(18, 18, 1, 1, 0, ParticleTypes.SNOWFLAKE));
    }

    public FrostEffect(SpellShape shape) {
        super(shape, new FreezePayload());
    }
}
