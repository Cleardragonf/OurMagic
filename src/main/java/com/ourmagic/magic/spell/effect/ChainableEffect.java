package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public interface ChainableEffect extends SpellEffect {
    Optional<ChainStart> chainStart(SpellContext context);

    ParticleOptions chainParticle();

    void applyChainTarget(SpellContext context, LivingEntity target, Vec3 targetPos, float powerMultiplier);
}
