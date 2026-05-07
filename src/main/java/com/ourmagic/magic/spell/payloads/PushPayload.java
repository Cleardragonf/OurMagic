package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;

import com.ourmagic.magic.Spell;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

import java.util.Set;

public class PushPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.CLOUD;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_MULTISTRIKE);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty()) {
            return false;
        }

        var entity = target.entity().get();
        context.beam(target.position(), ParticleTypes.CLOUD);
        context.ring(entity.position().add(0, 0.7D, 0), ParticleTypes.POOF, 1.0D, 20);
        entity.push(context.player().getLookAngle().x * 1.7D * context.utilityPower(), 0.45D, context.player().getLookAngle().z * 1.7D * context.utilityPower());
        entity.hurtMarked = true;
        return true;
    }
}
