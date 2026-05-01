package com.ourmagic.magic.spell.effect;

public class BubbleEffect extends ComposedSpellEffect {
    public BubbleEffect() {
        this(SpellShapes.self());
    }

    public BubbleEffect(SpellShape shape) {
        super(shape, new BubblePayload());
    }
}
