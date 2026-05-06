package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class BubblePayload implements PayloadEffect {
    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        living.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, Math.round(20 * 20 * context.data().activeUtilityMultiplier() * context.durationMultiplier() * context.modifierPower()), 0));
        context.burst(living.position().add(0, 1.0D, 0), ParticleTypes.BUBBLE_POP, 45, 0.75D, 0.04D);
        context.ring(living.position().add(0, 0.2D, 0), ParticleTypes.BUBBLE, 1.0D, 24);
        return true;
    }
}
