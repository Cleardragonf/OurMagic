package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.BubbleEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;

public class BubbleSpell extends BaseSpell {
    public BubbleSpell() {
        this("bubble", SpellShapes.self());
    }

    public BubbleSpell(String key, SpellShape shape) {
        super(key, 10, 30, new BubbleEffect(shape), UPGRADE_DURATION);
    }
}
