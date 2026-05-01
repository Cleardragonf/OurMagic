package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.BaseFireBlock;

public class FirePlacementPayload implements PayloadEffect {
    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.block().isEmpty() || !context.level().isEmptyBlock(target.block().get())) {
            return false;
        }

        context.beam(target.position(), ParticleTypes.FLAME);
        context.level().setBlockAndUpdate(target.block().get(), BaseFireBlock.getState(context.level(), target.block().get()));
        context.burst(target.block().get().getCenter(), ParticleTypes.FLAME, 20, 0.35D, 0.02D);
        return true;
    }
}
