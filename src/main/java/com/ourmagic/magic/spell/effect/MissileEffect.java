package com.ourmagic.magic.spell.effect;

public class MissileEffect extends ComposedSpellEffect {
    public MissileEffect() {
        this(SpellShapes.self());
    }

    public MissileEffect(SpellShape shape) {
        super(shape, new MissilePayload());
    }
}
