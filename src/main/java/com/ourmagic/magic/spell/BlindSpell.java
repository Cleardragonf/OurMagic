package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.BlindEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;

public class BlindSpell extends BaseSpell {
    public BlindSpell() {
        this("blind", SpellShapes.lookedLiving(18));
    }

    public BlindSpell(String key, SpellShape shape) {
        super(key, 15, 35, new BlindEffect(shape), UPGRADE_DURATION, UPGRADE_CHAINING);
    }
}
