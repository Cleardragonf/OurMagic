package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.item.ItemEntity;

public class GatherPayload implements PayloadEffect {
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
