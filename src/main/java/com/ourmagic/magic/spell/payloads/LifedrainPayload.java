package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.MagicDamageSources;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class LifedrainPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.DAMAGE_INDICATOR;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DAMAGE, Spell.UPGRADE_RANGE);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living) || living == context.player()) {
            return false;
        }

        float amount = 4.0F * context.damagePower();
        boolean hurt = living.hurt(MagicDamageSources.playerMagic(context.level(), context.player()), amount);
        if (!hurt) {
            return false;
        }

        context.player().heal(amount * 0.55F);
        context.beam(target.position(), ParticleTypes.DAMAGE_INDICATOR);
        context.burst(living.position().add(0, living.getBbHeight() * 0.6D, 0), ParticleTypes.DAMAGE_INDICATOR, 16, 0.35D, 0.03D);
        context.burst(context.player().position().add(0, 1.0D, 0), ParticleTypes.HEART, 5, 0.35D, 0.02D);
        return true;
    }
}
