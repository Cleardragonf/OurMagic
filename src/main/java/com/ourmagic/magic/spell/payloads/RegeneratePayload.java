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

public class RegeneratePayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.HAPPY_VILLAGER;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_RANGE, Spell.UPGRADE_DURATION);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        boolean self = living == context.player();
        living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Math.round((self ? 160 : 120) * context.data().activeUtilityMultiplier() * context.durationMultiplier()), self ? 1 : 0));
        if (self) {
            living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Math.round(120 * context.data().activeUtilityMultiplier() * context.durationMultiplier()), 0));
        }
        context.burst(living.position().add(0, 1.0D, 0), ParticleTypes.TOTEM_OF_UNDYING, self ? 55 : 25, self ? 0.8D : 0.6D, self ? 0.08D : 0.06D);
        return true;
    }
}
