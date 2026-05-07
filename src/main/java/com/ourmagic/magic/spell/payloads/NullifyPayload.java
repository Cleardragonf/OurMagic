package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.runtime.MagicStatusEffects;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;

import com.ourmagic.magic.Spell;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class NullifyPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.REVERSE_PORTAL;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_RANGE);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        MagicStatusEffects.nullify(living);
        context.beam(target.position(), ParticleTypes.REVERSE_PORTAL);
        context.burst(living.position().add(0, living.getBbHeight() * 0.5D, 0), ParticleTypes.REVERSE_PORTAL, 35, 0.55D, 0.08D);
        context.burst(living.position().add(0, living.getBbHeight() * 0.5D, 0), ParticleTypes.SMOKE, 20, 0.45D, 0.02D);
        return true;
    }
}
