package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.MissileEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;

public class MissileSpell extends BaseSpell {
    public MissileSpell() {
        this("missile", SpellShapes.self());
    }

    public MissileSpell(String key, SpellShape shape) {
        super(key, 8, 12, new MissileEffect(shape), UPGRADE_MULTISTRIKE);
    }
}
