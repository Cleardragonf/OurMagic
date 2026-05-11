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

public class LevitatePayload implements PayloadEffect {
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

        boolean self = living == context.player();
        if (self) {
            context.burst(living.position().add(0, 0.4D, 0), ParticleTypes.END_ROD, 22, 0.5D, 0.03D);
        } else {
            context.beam(target.position(), ParticleTypes.END_ROD);
            context.ring(living.position().add(0, 0.2D, 0), ParticleTypes.CLOUD, 0.8D, 20);
        }
        living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, Math.round((self ? 60 : 80) * context.data().activeUtilityMultiplier() * context.durationMultiplier()), 0));
        return true;
    }
}
