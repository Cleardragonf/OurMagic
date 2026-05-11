package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.runtime.Scrying;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class ScryPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.GLOW;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DURATION, Spell.UPGRADE_RANGE);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (context.selfShape()) {
            return Scrying.openSelection(context.player(), context.durationMultiplier());
        }

        if (target.entity().isPresent() && target.entity().get() instanceof LivingEntity living) {
            if (living instanceof ServerPlayer && Scrying.markPlayer(context.player(), living)) {
                living.addEffect(new MobEffectInstance(MobEffects.GLOWING, Math.round(20 * 8 * context.durationMultiplier() * context.utilityPower()), 0));
                context.beam(target.position(), ParticleTypes.GLOW);
                context.burst(target.position(), ParticleTypes.GLOW, 24, 0.45D, 0.03D);
                return true;
            }

            living.addEffect(new MobEffectInstance(MobEffects.GLOWING, Math.round(20 * 20 * context.durationMultiplier() * context.utilityPower()), 0));
            context.player().displayClientMessage(Component.literal(String.format("Scry: %.0f %.0f %.0f", living.getX(), living.getY(), living.getZ())), true);
            context.beam(target.position(), ParticleTypes.GLOW);
            context.burst(target.position(), ParticleTypes.GLOW, 24, 0.45D, 0.03D);
            return true;
        }

        Scrying.markLocation(context.player(), target.position());
        context.beam(target.position(), ParticleTypes.GLOW);
        context.ring(target.position(), ParticleTypes.GLOW, 0.75D, 24);
        return true;
    }
}
