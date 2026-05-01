package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.FireEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;

public class FireSpell extends BaseSpell {
    public FireSpell() {
        this("fire", SpellShapes.lookedBlockAdjacent(18));
    }

    public FireSpell(String key, SpellShape shape) {
        super(key, 12, 20, new FireEffect(shape));
    }
}
