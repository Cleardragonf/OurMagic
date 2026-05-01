package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.LevitateEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;

public class LevitateSpell extends BaseSpell {
    public LevitateSpell() {
        this("levitate", SpellShapes.lookedLivingOrSelf(20));
    }

    public LevitateSpell(String key, SpellShape shape) {
        super(key, 22, 45, new LevitateEffect(shape), UPGRADE_DURATION);
    }
}
