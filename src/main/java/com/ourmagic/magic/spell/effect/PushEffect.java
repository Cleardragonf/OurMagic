package com.ourmagic.magic.spell.effect;

public class PushEffect extends ComposedSpellEffect {
    public PushEffect() {
        this(SpellShapes.lookedLiving(18));
    }

    public PushEffect(SpellShape shape) {
        super(shape, new PushPayload());
    }
}
