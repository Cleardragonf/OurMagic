package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.GatherEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;
import net.minecraft.core.particles.ParticleTypes;

public class GatherSpell extends BaseSpell {
    public GatherSpell() {
        this("gather", SpellShapes.itemsAroundSelf(10, ParticleTypes.ENCHANT));
    }

    public GatherSpell(String key, SpellShape shape) {
        super(key, 15, 30, new GatherEffect(shape));
    }
}
