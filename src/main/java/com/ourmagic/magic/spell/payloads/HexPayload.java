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

public class HexPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.WITCH;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DAMAGE, Spell.UPGRADE_DURATION);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        float damageMultiplier = 1.35F + context.data().activeUpgradeLevel(Spell.UPGRADE_DAMAGE) * 0.08F;
        MagicStatusEffects.hex(living, Math.round(20 * 12 * context.durationMultiplier() * context.utilityPower()), damageMultiplier);
        context.beam(target.position(), ParticleTypes.WITCH);
        context.burst(living.position().add(0, living.getBbHeight() * 0.55D, 0), ParticleTypes.WITCH, 26, 0.45D, 0.03D);
        return true;
    }
}
