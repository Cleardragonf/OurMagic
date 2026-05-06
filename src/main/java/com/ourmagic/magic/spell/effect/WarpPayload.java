package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;

public class WarpPayload implements PayloadEffect {
    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof ServerPlayer player)) {
            return false;
        }

        int duration = Math.round(120 * context.data().activeUtilityMultiplier() * context.durationMultiplier() * context.modifierPower());
        MagicStatusEffects.warp(player, duration);
        context.beam(target.position(), ParticleTypes.PORTAL);
        context.burst(player.position().add(0, player.getBbHeight() * 0.5D, 0), ParticleTypes.PORTAL, 55, 0.65D, 0.1D);
        return true;
    }
}
