package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.item.ItemEntity;

public class GatherPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.ENCHANT;
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (target.entity().isPresent() && target.entity().get() == context.player()) {
            return true;
        }
        if (target.entity().isEmpty() || !(target.entity().get() instanceof ItemEntity item)) {
            return false;
        }

        context.beam(item.position().add(0, 0.25D, 0), ParticleTypes.ENCHANT);
        item.setDeltaMovement(context.player().position().subtract(item.position()).normalize().scale(0.65D));
        item.hurtMarked = true;
        return true;
    }
}
