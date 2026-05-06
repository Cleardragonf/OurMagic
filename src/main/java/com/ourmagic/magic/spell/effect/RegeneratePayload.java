package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class RegeneratePayload implements PayloadEffect {
    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        boolean self = living == context.player();
        living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Math.round((self ? 160 : 120) * context.data().activeUtilityMultiplier() * context.durationMultiplier() * context.modifierPower()), self ? 1 : 0));
        if (self) {
            living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Math.round(120 * context.data().activeUtilityMultiplier() * context.durationMultiplier() * context.modifierPower()), 0));
        }
        context.burst(living.position().add(0, 1.0D, 0), ParticleTypes.TOTEM_OF_UNDYING, self ? 55 : 25, self ? 0.8D : 0.6D, self ? 0.08D : 0.06D);
        return true;
    }
}
