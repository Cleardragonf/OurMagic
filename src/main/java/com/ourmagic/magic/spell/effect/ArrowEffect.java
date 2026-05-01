package com.ourmagic.magic.spell.effect;

public class ArrowEffect extends ComposedSpellEffect {
    public ArrowEffect() {
        this(SpellShapes.self());
    }

    public ArrowEffect(SpellShape shape) {
        super(shape, new ArrowPayload());
    }
}
