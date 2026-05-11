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

public class WarpPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.PORTAL;
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

        int duration = Math.round(120 * context.data().activeUtilityMultiplier() * context.durationMultiplier());
        MagicStatusEffects.warp(player, duration);
        context.beam(target.position(), ParticleTypes.PORTAL);
        context.burst(player.position().add(0, player.getBbHeight() * 0.5D, 0), ParticleTypes.PORTAL, 55, 0.65D, 0.1D);
        return true;
    }
}
