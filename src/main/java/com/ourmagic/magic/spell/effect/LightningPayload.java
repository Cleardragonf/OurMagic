package com.ourmagic.magic.spell.effect;

import com.ourmagic.magic.SpellEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

public class LightningPayload implements PayloadEffect {
    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        Vec3 at = spreadTarget(context, target.position());
        if (context.castIndex() == 0) {
            context.beam(target.position(), ParticleTypes.ELECTRIC_SPARK);
            context.burst(target.position(), ParticleTypes.ELECTRIC_SPARK, 60, 0.9D, 0.12D);
            context.burst(target.position(), ParticleTypes.FLASH, 1, 0.0D, 0.0D);
        }
        SpellEffects.strikeLightning(context.level(), context.player(), at);
        return true;
    }

    private static Vec3 spreadTarget(SpellContext context, Vec3 target) {
        if (context.castIndex() == 0) {
            return target;
        }
        double spread = 0.7D + context.castIndex() * 0.2D;
        return target.add((context.player().getRandom().nextDouble() - 0.5D) * spread, 0, (context.player().getRandom().nextDouble() - 0.5D) * spread);
    }
}
