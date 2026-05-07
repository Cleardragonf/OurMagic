package com.ourmagic.magic.spell.shapes;

import net.minecraft.core.particles.ParticleOptions;

import java.util.Optional;

public record SpellShape(TargetSelector targetSelector, AreaSelector areaSelector, Optional<ParticleOptions> areaRingParticle) {
    public SpellShape(TargetSelector targetSelector, AreaSelector areaSelector) {
        this(targetSelector, areaSelector, Optional.empty());
    }

    public SpellShape(TargetSelector targetSelector, AreaSelector areaSelector, ParticleOptions areaRingParticle) {
        this(targetSelector, areaSelector, Optional.ofNullable(areaRingParticle));
    }
}
