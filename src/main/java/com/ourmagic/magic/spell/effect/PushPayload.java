package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;

public class PushPayload implements PayloadEffect {
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
