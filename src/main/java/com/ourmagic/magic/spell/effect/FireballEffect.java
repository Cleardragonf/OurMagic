package com.ourmagic.magic.spell.effect;

public class FireballEffect extends ComposedSpellEffect {
    public FireballEffect() {
        this(SpellShapes.self());
    }

    public FireballEffect(SpellShape shape) {
        super(shape, new FireballPayload());
    }
}
