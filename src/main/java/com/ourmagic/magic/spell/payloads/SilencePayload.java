package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.MagicStatusEffects;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class SilencePayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.SMOKE;
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

        int duration = Math.round(100 * context.durationMultiplier() * context.utilityPower());
        MagicStatusEffects.silence(living, duration);
        context.beam(target.position(), ParticleTypes.SMOKE);
        context.burst(living.position().add(0, living.getBbHeight() * 0.75D, 0), ParticleTypes.SMOKE, 30, 0.35D, 0.02D);
        return true;
    }
}
