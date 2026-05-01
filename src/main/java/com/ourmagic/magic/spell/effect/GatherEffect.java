package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
public class GatherEffect extends ComposedSpellEffect {
    public GatherEffect() {
        this(SpellShapes.itemsAroundSelf(10, ParticleTypes.ENCHANT));
    }

    public GatherEffect(SpellShape shape) {
        super(shape, new GatherPayload());
    }
}
