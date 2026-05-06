package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

public class FreezePayload implements PayloadEffect {
    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        boolean applied = false;
        if (target.entity().isPresent() && target.entity().get() instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.round(100 * context.data().activeUtilityMultiplier() * context.durationMultiplier() * context.modifierPower()), 2));
            applied = true;
        }
        if (target.block().isPresent() && context.level().getBlockState(target.block().get()).is(Blocks.WATER)) {
            context.level().setBlockAndUpdate(target.block().get(), Blocks.FROSTED_ICE.defaultBlockState());
            applied = true;
        }

        if (target.entity().isEmpty() && target.block().isEmpty()) {
            applied = true;
        }

        if (applied) {
            context.beam(target.position(), ParticleTypes.SNOWFLAKE);
            context.burst(target.position(), ParticleTypes.SNOWFLAKE, 35, 0.7D, 0.04D);
        }
        return applied;
    }
}
