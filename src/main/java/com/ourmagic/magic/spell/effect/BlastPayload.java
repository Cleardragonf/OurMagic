package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level.ExplosionInteraction;

public class BlastPayload implements PayloadEffect {
    private final ExplosionPayload explosion = new ExplosionPayload(1.5F, ExplosionInteraction.NONE);

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (context.castIndex() == 0) {
            context.sparkleBeam(target.position());
        }
        context.burst(target.position(), ParticleTypes.EXPLOSION, 3, 0.25D, 0.0D);
        return explosion.apply(context, target);
    }
}
