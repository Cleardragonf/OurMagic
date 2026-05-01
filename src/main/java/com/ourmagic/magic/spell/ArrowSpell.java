package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.ArrowEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;

public class ArrowSpell extends BaseSpell {
    public ArrowSpell() {
        this("arrow", SpellShapes.self());
    }

    public ArrowSpell(String key, SpellShape shape) {
        super(key, 10, 14, new ArrowEffect(shape), UPGRADE_MULTISTRIKE);
    }
}
