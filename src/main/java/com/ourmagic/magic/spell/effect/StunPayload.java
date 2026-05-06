package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;

public class StunPayload implements PayloadEffect {
    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof ServerPlayer player)) {
            return false;
        }

        int duration = Math.round(80 * context.data().activeUtilityMultiplier() * context.durationMultiplier() * context.modifierPower());
        MagicStatusEffects.stun(player, duration);
        context.beam(target.position(), ParticleTypes.WITCH);
        context.burst(player.position().add(0, player.getBbHeight() * 0.85D, 0), ParticleTypes.CRIT, 20, 0.4D, 0.04D);
        context.burst(player.position().add(0, player.getBbHeight() * 0.5D, 0), ParticleTypes.WITCH, 18, 0.35D, 0.02D);
        return true;
    }
}
