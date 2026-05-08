package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.DelayedSpellCasts;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class OverloadPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.ELECTRIC_SPARK;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DAMAGE, Spell.UPGRADE_DURATION);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living) || !(context.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        int delay = Math.round(35 * context.durationMultiplier());
        context.beam(target.position(), ParticleTypes.ELECTRIC_SPARK);
        context.burst(living.position().add(0, living.getBbHeight() * 0.6D, 0), ParticleTypes.ELECTRIC_SPARK, 22, 0.4D, 0.03D);
        DelayedSpellCasts.schedule(serverLevel, delay, () -> {
            if (!living.isAlive()) {
                return;
            }
            living.hurt(serverLevel.damageSources().magic(), 7.0F * context.damagePower());
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(), 55, 0.65D, 0.65D, 0.65D, 0.08D);
            serverLevel.sendParticles(ParticleTypes.FLASH, living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        });
        return true;
    }
}
