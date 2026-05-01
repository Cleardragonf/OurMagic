package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.HealEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;
import net.minecraft.core.particles.ParticleTypes;

public class HealSpell extends BaseSpell {
    public HealSpell() {
        this("heal", SpellShapes.playersAroundSelf(4, ParticleTypes.HAPPY_VILLAGER));
    }

    public HealSpell(String key, SpellShape shape) {
        super(key, 20, 60, new HealEffect(shape), UPGRADE_RANGE);
    }
}
