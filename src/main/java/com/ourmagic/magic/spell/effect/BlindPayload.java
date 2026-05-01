package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class BlindPayload implements PayloadEffect {
    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        context.beam(target.position(), ParticleTypes.SMOKE);
        context.burst(living.position().add(0, living.getBbHeight() * 0.75D, 0), ParticleTypes.SQUID_INK, 25, 0.45D, 0.02D);
        living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.round(100 * context.data().activeUtilityMultiplier() * context.durationMultiplier() * context.modifierPower()), 0));
        return true;
    }
}
