package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.PushEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;

public class PushSpell extends BaseSpell {
    public PushSpell() {
        this("push", SpellShapes.lookedLiving(18));
    }

    public PushSpell(String key, SpellShape shape) {
        super(key, 12, 18, new PushEffect(shape), UPGRADE_MULTISTRIKE);
    }
}
