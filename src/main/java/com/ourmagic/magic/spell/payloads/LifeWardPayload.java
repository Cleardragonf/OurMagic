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

public class LifeWardPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.TOTEM_OF_UNDYING;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DURATION, Spell.UPGRADE_HEALING);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        int duration = Math.round(20 * 24 * context.durationMultiplier() * context.utilityPower());
        MagicStatusEffects.lifeWard(living, duration, context.healingPower());
        context.ring(living.position().add(0, living.getBbHeight() * 0.55D, 0), ParticleTypes.TOTEM_OF_UNDYING, Math.max(0.9D, living.getBbWidth() + 0.45D), 38);
        context.burst(living.position().add(0, living.getBbHeight() * 0.8D, 0), ParticleTypes.HAPPY_VILLAGER, 26, 0.5D, 0.04D);
        return true;
    }
}
