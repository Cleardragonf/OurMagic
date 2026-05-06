package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

public class NullifyPayload implements PayloadEffect {
    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        MagicStatusEffects.nullify(living);
        context.beam(target.position(), ParticleTypes.REVERSE_PORTAL);
        context.burst(living.position().add(0, living.getBbHeight() * 0.5D, 0), ParticleTypes.REVERSE_PORTAL, 35, 0.55D, 0.08D);
        context.burst(living.position().add(0, living.getBbHeight() * 0.5D, 0), ParticleTypes.SMOKE, 20, 0.45D, 0.02D);
        return true;
    }
}
