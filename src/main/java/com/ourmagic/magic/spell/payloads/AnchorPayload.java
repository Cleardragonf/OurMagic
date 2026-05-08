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

public class AnchorPayload implements PayloadEffect {
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

        MagicStatusEffects.anchor(living, Math.round(80 * context.durationMultiplier() * context.utilityPower()));
        context.beam(target.position(), ParticleTypes.ENCHANT);
        context.ring(living.position().add(0, 0.12D, 0), ParticleTypes.ENCHANT, Math.max(0.8D, living.getBbWidth() + 0.35D), 30);
        context.burst(living.position().add(0, 0.4D, 0), ParticleTypes.CRIT, 16, 0.35D, 0.02D);
        return true;
    }
}
