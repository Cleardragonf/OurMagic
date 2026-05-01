package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.LightningEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;

public class LightningSpell extends BaseSpell {
    public LightningSpell() {
        this("lightning", SpellShapes.lookedLivingOrPoint(30, 20));
    }

    public LightningSpell(String key, SpellShape shape) {
        super(key, 35, 90, new LightningEffect(shape), UPGRADE_CHAINING, UPGRADE_MULTISTRIKE);
    }
}
