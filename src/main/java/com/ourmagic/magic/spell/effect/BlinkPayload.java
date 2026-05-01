package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;

public class BlinkPayload implements PayloadEffect {
    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        var before = context.player().position().add(0, 1.0D, 0);
        var destination = target.position().subtract(context.player().getLookAngle().normalize().scale(1.2D));
        context.beam(destination.add(0, 1.0D, 0), ParticleTypes.PORTAL);
        context.burst(before, ParticleTypes.REVERSE_PORTAL, 35, 0.55D, 0.08D);
        context.player().teleportTo(destination.x, destination.y, destination.z);
        context.player().resetFallDistance();
        context.burst(destination.add(0, 1.0D, 0), ParticleTypes.PORTAL, 45, 0.65D, 0.08D);
        return true;
    }
}
