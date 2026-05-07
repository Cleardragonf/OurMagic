package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;

import com.ourmagic.magic.Spell;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import java.util.Set;

public class BlinkPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return context.selfShape() ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.PORTAL;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_RANGE);
    }

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
