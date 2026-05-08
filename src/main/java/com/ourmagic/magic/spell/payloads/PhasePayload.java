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

public class PhasePayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.PORTAL;
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

        int duration = Math.round(80 * context.durationMultiplier() * context.utilityPower());
        living.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0));
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1));
        living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1));
        living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0));
        context.burst(living.position().add(0, living.getBbHeight() * 0.5D, 0), ParticleTypes.PORTAL, 65, 0.6D, 0.12D);
        return true;
    }
}
