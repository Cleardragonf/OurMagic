package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class MobEffectPayload implements PayloadEffect {
    private final MobEffect effect;
    private final int baseTicks;
    private final int amplifier;
    private final ParticleOptions particle;
    private final int particleCount;

    public MobEffectPayload(MobEffect effect, int baseTicks, int amplifier, ParticleOptions particle, int particleCount) {
        this.effect = effect;
        this.baseTicks = baseTicks;
        this.amplifier = amplifier;
        this.particle = particle;
        this.particleCount = particleCount;
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        living.addEffect(new MobEffectInstance(effect, Math.round(baseTicks * context.data().activeUtilityMultiplier() * context.durationMultiplier()), amplifier));
        context.burst(living.position().add(0, 1.0D, 0), particle, particleCount, 0.6D, 0.06D);
        return true;
    }
}
