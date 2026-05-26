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

public class FirePayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.FLAME;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DAMAGE, Spell.UPGRADE_DURATION, Spell.UPGRADE_MULTISTRIKE, Spell.UPGRADE_CHAINING);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        int burnSeconds = Math.max(2, Math.round(4.0F * context.durationMultiplier() * context.utilityPower()));
        living.setSecondsOnFire(burnSeconds);
        living.hurt(MagicDamageSources.playerMagic(context.level(), context.player()), 2.0F * context.damagePower());
        context.beam(target.position(), ParticleTypes.FLAME);
        context.burst(living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D), ParticleTypes.FLAME, 22, 0.35D, 0.04D);
        context.burst(living.position().add(0.0D, 0.2D, 0.0D), ParticleTypes.LAVA, 6, 0.2D, 0.03D);
        return true;
    }
}
