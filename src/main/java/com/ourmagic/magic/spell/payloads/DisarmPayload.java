package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class DisarmPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.CRIT;
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

        ItemStack held = living.getMainHandItem();
        InteractionHand hand = InteractionHand.MAIN_HAND;
        if (held.isEmpty()) {
            held = living.getOffhandItem();
            hand = InteractionHand.OFF_HAND;
        }
        if (held.isEmpty()) {
            return false;
        }

        ItemStack dropped = held.copy();
        living.setItemInHand(hand, ItemStack.EMPTY);
        ItemEntity item = new ItemEntity(context.level(), living.getX(), living.getY() + living.getBbHeight() * 0.6D, living.getZ(), dropped);
        item.setDeltaMovement(context.player().getLookAngle().scale(0.25D).add(0, 0.25D, 0));
        context.level().addFreshEntity(item);
        context.beam(target.position(), ParticleTypes.CRIT);
        context.burst(living.position().add(0, living.getBbHeight() * 0.7D, 0), ParticleTypes.CRIT, 18, 0.35D, 0.05D);
        return true;
    }
}
