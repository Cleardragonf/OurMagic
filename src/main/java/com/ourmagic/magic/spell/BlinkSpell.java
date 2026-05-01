package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.BlinkEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;

public class BlinkSpell extends BaseSpell {
    public BlinkSpell() {
        this("blink", SpellShapes.lookedPointFromPlayerPosition(28, 16));
    }

    public BlinkSpell(String key, SpellShape shape) {
        super(key, 28, 70, new BlinkEffect(shape), UPGRADE_RANGE);
    }
}
