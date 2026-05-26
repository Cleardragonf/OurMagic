package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class FortifyPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.END_ROD;
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

        int duration = Math.round(20 * 18 * context.durationMultiplier() * context.utilityPower());
        living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1));
        living.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 1));
        context.ring(living.position().add(0, living.getBbHeight() * 0.45D, 0), ParticleTypes.END_ROD, Math.max(0.9D, living.getBbWidth() + 0.45D), 34);
        context.burst(living.position().add(0, living.getBbHeight() * 0.7D, 0), ParticleTypes.CRIT, 24, 0.45D, 0.025D);
        return true;
    }
}
