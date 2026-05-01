package com.ourmagic.magic.spell;

import com.ourmagic.magic.spell.effect.FrostEffect;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;
import net.minecraft.core.particles.ParticleTypes;

public class FrostSpell extends BaseSpell {
    public FrostSpell() {
        this("frost", SpellShapes.waterBlocksAroundLookedLivingOrPoint(18, 18, 1, 1, 0, ParticleTypes.SNOWFLAKE));
    }

    public FrostSpell(String key, SpellShape shape) {
        super(key, 14, 24, new FrostEffect(shape), UPGRADE_DURATION);
    }
}
