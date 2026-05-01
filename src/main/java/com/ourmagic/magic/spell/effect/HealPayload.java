package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

public class HealPayload implements PayloadEffect {
    private final float selfAmount;
    private final float otherAmount;
    private final int particleCount;

    public HealPayload(float amount, int particleCount) {
        this(amount, amount, particleCount);
    }

    public HealPayload(float selfAmount, float otherAmount, int particleCount) {
        this.selfAmount = selfAmount;
        this.otherAmount = otherAmount;
        this.particleCount = particleCount;
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        float amount = living == context.player() ? selfAmount : otherAmount;
        living.heal(amount * context.data().power() * context.data().activeUtilityMultiplier());
        context.burst(living.position().add(0, 1.1D, 0), ParticleTypes.HEART, particleCount, 0.55D, 0.02D);
        return true;
    }
}
