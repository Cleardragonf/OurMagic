package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import java.util.Set;

public interface PayloadEffect {
    boolean apply(SpellContext context, SpellTarget target);

    default ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.ENCHANT;
    }

    default Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of();
    }
}
