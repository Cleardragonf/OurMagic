package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.MagicStatusEffects;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class FallguardPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.CLOUD;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DURATION);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        int duration = Math.round(20 * 25 * context.durationMultiplier() * context.utilityPower());
        MagicStatusEffects.fallguard(living, duration);
        living.fallDistance = 0.0F;
        living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0));
        context.burst(living.position().add(0, living.getBbHeight() * 0.45D, 0), ParticleTypes.CLOUD, 28, 0.55D, 0.035D);
        context.ring(living.position().add(0, 0.18D, 0), ParticleTypes.CLOUD, Math.max(0.9D, living.getBbWidth() + 0.4D), 26);
        return true;
    }
}
