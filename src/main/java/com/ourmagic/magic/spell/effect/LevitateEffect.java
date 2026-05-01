package com.ourmagic.magic.spell.effect;

public class LevitateEffect extends ComposedSpellEffect {
    public LevitateEffect() {
        this(SpellShapes.lookedLivingOrSelf(20));
    }

    public LevitateEffect(SpellShape shape) {
        super(shape, new LevitatePayload());
    }
}
