package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.BlastEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;

public class BlastSpell extends BaseSpell {
    public BlastSpell() {
        this("blast", SpellShapes.lookedPoint(24, 16));
    }

    public BlastSpell(String key, SpellShape shape) {
        super(key, 24, 50, new BlastEffect(shape), UPGRADE_MULTISTRIKE);
    }
}
