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

public class ReflectPayload implements PayloadEffect {
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

        MagicStatusEffects.reflect(living, Math.round(20 * 12 * context.durationMultiplier() * context.utilityPower()));
        context.ring(living.position().add(0, living.getBbHeight() * 0.55D, 0), ParticleTypes.ENCHANT, Math.max(0.8D, living.getBbWidth() + 0.4D), 36);
        context.burst(living.position().add(0, living.getBbHeight() * 0.65D, 0), ParticleTypes.END_ROD, 20, 0.45D, 0.02D);
        return true;
    }
}
