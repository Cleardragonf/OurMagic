package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;

import com.ourmagic.magic.Spell;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class BlindPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.SQUID_INK;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DURATION, Spell.UPGRADE_CHAINING);
    }

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
