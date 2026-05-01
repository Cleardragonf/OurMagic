package com.ourmagic.magic.spell.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public record SpellTarget(Optional<Entity> entity, Optional<BlockPos> block, Vec3 position) {
    public static SpellTarget entity(Entity entity) {
        return new SpellTarget(Optional.of(entity), Optional.empty(), entity.position().add(0, entity.getBbHeight() * 0.5D, 0));
    }

    public static SpellTarget living(LivingEntity entity) {
        return entity(entity);
    }

    public static SpellTarget block(BlockPos block) {
        return new SpellTarget(Optional.empty(), Optional.of(block), block.getCenter());
    }

    public static SpellTarget point(Vec3 position) {
        return new SpellTarget(Optional.empty(), Optional.empty(), position);
    }
}
