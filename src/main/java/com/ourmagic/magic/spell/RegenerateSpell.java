package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.RegenerateEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;
import net.minecraft.core.particles.ParticleTypes;

public class RegenerateSpell extends BaseSpell {
    public RegenerateSpell() {
        this("regenerate", SpellShapes.playersAroundSelf(4, ParticleTypes.HAPPY_VILLAGER));
    }

    public RegenerateSpell(String key, SpellShape shape) {
        super(key, 35, 100, new RegenerateEffect(shape), UPGRADE_RANGE);
    }
}
