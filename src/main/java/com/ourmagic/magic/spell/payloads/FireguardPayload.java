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

public class FireguardPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.FLAME;
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

        int duration = Math.round(20 * 28 * context.durationMultiplier() * context.utilityPower());
        living.clearFire();
        living.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0));
        context.burst(living.position().add(0, living.getBbHeight() * 0.5D, 0), ParticleTypes.FLAME, 18, 0.5D, 0.03D);
        context.ring(living.position().add(0, 0.12D, 0), ParticleTypes.SMALL_FLAME, Math.max(0.85D, living.getBbWidth() + 0.35D), 24);
        return true;
    }
}
