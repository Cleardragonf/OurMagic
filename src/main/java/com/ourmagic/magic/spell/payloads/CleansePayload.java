package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.MagicStatusEffects;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class CleansePayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.HAPPY_VILLAGER;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_RANGE);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        MagicStatusEffects.nullify(living);
        living.removeEffect(MobEffects.POISON);
        living.removeEffect(MobEffects.WITHER);
        living.removeEffect(MobEffects.HUNGER);
        living.removeEffect(MobEffects.CONFUSION);
        living.removeEffect(MobEffects.DIG_SLOWDOWN);
        living.removeEffect(MobEffects.WEAKNESS);
        context.burst(living.position().add(0, living.getBbHeight() * 0.6D, 0), ParticleTypes.HAPPY_VILLAGER, 20, 0.45D, 0.02D);
        return true;
    }
}
