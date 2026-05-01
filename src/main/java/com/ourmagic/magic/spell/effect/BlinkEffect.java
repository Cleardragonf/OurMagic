package com.ourmagic.magic.spell.effect;

public class BlinkEffect extends ComposedSpellEffect {
    public BlinkEffect() {
        this(SpellShapes.lookedPointFromPlayerPosition(28, 16));
    }

    public BlinkEffect(SpellShape shape) {
        super(shape, new BlinkPayload());
    }
}
