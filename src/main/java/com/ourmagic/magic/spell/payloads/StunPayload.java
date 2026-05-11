package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.runtime.MagicStatusEffects;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;

import com.ourmagic.magic.Spell;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

public class StunPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.WITCH;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DURATION);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof ServerPlayer player)) {
            return false;
        }

        int duration = Math.round(80 * context.data().activeUtilityMultiplier() * context.durationMultiplier());
        MagicStatusEffects.stun(player, duration);
        context.beam(target.position(), ParticleTypes.WITCH);
        context.burst(player.position().add(0, player.getBbHeight() * 0.85D, 0), ParticleTypes.CRIT, 20, 0.4D, 0.04D);
        context.burst(player.position().add(0, player.getBbHeight() * 0.5D, 0), ParticleTypes.WITCH, 18, 0.35D, 0.02D);
        return true;
    }
}
