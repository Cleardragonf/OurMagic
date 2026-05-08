package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.MagicStatusEffects;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Mob;

import java.util.Set;

public class CharmPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.HEART;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DURATION);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof Mob mob)) {
            return false;
        }

        int duration = Math.round(140 * context.durationMultiplier() * context.utilityPower());
        MagicStatusEffects.charm(mob, context.player(), duration);
        context.beam(target.position(), ParticleTypes.HEART);
        context.burst(mob.position().add(0, mob.getBbHeight() * 0.75D, 0), ParticleTypes.HEART, 10, 0.45D, 0.02D);
        return true;
    }
}
