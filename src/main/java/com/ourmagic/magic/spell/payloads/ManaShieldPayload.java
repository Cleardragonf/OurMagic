package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.MagicStatusEffects;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

public class ManaShieldPayload implements PayloadEffect {
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
        if (target.entity().isEmpty() || !(target.entity().get() instanceof ServerPlayer player)) {
            return false;
        }

        int duration = Math.round(20 * 18 * context.durationMultiplier() * context.utilityPower());
        MagicStatusEffects.manaShield(player, duration, Math.max(0.35F, 0.45F * context.utilityPower()));
        context.ring(player.position().add(0, player.getBbHeight() * 0.55D, 0), ParticleTypes.ENCHANT, Math.max(0.9D, player.getBbWidth() + 0.45D), 42);
        context.burst(player.position().add(0, player.getBbHeight() * 0.7D, 0), ParticleTypes.END_ROD, 26, 0.45D, 0.03D);
        return true;
    }
}
