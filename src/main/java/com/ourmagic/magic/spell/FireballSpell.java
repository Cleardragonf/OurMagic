package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.FireballEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;

public class FireballSpell extends BaseSpell {
    public FireballSpell() {
        this("fireball", SpellShapes.self());
    }

    public FireballSpell(String key, SpellShape shape) {
        super(key, 18, 30, new FireballEffect(shape), UPGRADE_MULTISTRIKE);
    }
}
