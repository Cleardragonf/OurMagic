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

public class StunPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.WITCH;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DURATION, Spell.UPGRADE_RANGE, Spell.UPGRADE_CHAINING);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity entity)) {
            return false;
        }

        int duration = Math.round(80 * context.data().activeUtilityMultiplier() * context.durationMultiplier() * context.modifierPower());
        MagicStatusEffects.stun(entity, duration);
        context.beam(target.position(), ParticleTypes.WITCH);
        context.burst(entity.position().add(0, entity.getBbHeight() * 0.85D, 0), ParticleTypes.CRIT, 20, 0.4D, 0.04D);
        context.burst(entity.position().add(0, entity.getBbHeight() * 0.5D, 0), ParticleTypes.WITCH, 18, 0.35D, 0.02D);
        return true;
    }
}
