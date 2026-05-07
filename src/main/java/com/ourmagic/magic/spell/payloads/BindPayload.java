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

public class BindPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.ENCHANT;
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

        int duration = Math.round(100 * context.data().activeUtilityMultiplier() * context.durationMultiplier() * context.modifierPower());
        MagicStatusEffects.bind(living, duration);
        context.beam(target.position(), ParticleTypes.ENCHANT);
        context.ring(living.position().add(0, 0.15D, 0), ParticleTypes.ENCHANT, Math.max(0.6D, living.getBbWidth()), 28);
        context.burst(living.position().add(0, living.getBbHeight() * 0.5D, 0), ParticleTypes.WITCH, 18, 0.35D, 0.01D);
        return true;
    }
}
