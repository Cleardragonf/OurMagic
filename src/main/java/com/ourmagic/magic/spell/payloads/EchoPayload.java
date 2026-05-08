package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.DelayedSpellCasts;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Set;

public class EchoPayload implements PayloadEffect {
    private static final int DELAY_TICKS = 18;
    private static final float ECHO_POWER = 0.55F;

    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.NOTE;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DURATION);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        context.burst(target.position(), ParticleTypes.NOTE, 8, 0.35D, 0.01D);
        return true;
    }

    static void scheduleEcho(SpellContext context, SpellTarget target, List<PayloadEffect> payloads) {
        if (!(context.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int delay = Math.round(DELAY_TICKS * context.durationMultiplier());
        DelayedSpellCasts.schedule(serverLevel, delay, () -> {
            SpellContext echoContext = context.withCastIteration(context.castIndex(), context.castCount(), context.modifierPower() * ECHO_POWER).withoutBeams();
            echoContext.burst(target.position(), ParticleTypes.NOTE, 12, 0.45D, 0.01D);
            for (PayloadEffect payload : payloads) {
                payload.apply(echoContext, target);
            }
        });
    }
}
