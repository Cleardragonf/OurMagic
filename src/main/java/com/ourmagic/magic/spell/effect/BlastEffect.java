package com.ourmagic.magic.spell.effect;

public class BlastEffect extends ComposedSpellEffect {
    public BlastEffect() {
        this(SpellShapes.lookedPoint(24, 16));
    }

    public BlastEffect(SpellShape shape) {
        super(shape, new BlastPayload());
    }
}
