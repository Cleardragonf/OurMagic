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

public class BlastguardPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.EXPLOSION;
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

        int duration = Math.round(20 * 18 * context.durationMultiplier() * context.utilityPower());
        MagicStatusEffects.blastguard(living, duration);
        context.ring(living.position().add(0, living.getBbHeight() * 0.45D, 0), ParticleTypes.EXPLOSION, Math.max(0.95D, living.getBbWidth() + 0.5D), 28);
        context.burst(living.position().add(0, living.getBbHeight() * 0.65D, 0), ParticleTypes.SMOKE, 32, 0.5D, 0.035D);
        return true;
    }
}
