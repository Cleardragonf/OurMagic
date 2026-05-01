package com.ourmagic.magic.spell.effect;

public class FireEffect extends ComposedSpellEffect {
    public FireEffect() {
        this(SpellShapes.lookedBlockAdjacent(18));
    }

    public FireEffect(SpellShape shape) {
        super(shape, new FirePlacementPayload());
    }
}
