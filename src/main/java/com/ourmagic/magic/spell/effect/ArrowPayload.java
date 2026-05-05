package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;

public class ArrowPayload implements PayloadEffect {
    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        Arrow arrow = new Arrow(context.level(), context.player());
        arrow.setBaseDamage(3.0D * context.damagePower());
        if (context.selfShape() || context.areaCast() && target.entity().isPresent() && target.entity().get() != context.player()) {
            Vec3 targetPos = overheadTarget(context, target);
            Vec3 start = targetPos.add((context.castIndex() - (context.castCount() - 1) / 2.0F) * 0.25D, 8.0D, 0.0D);
            arrow.setPos(start.x, start.y, start.z);
            arrow.shoot(targetPos.x - start.x, targetPos.y - start.y, targetPos.z - start.z, 2.6F, 0.15F);
            context.burst(start, ParticleTypes.ENCHANTED_HIT, 6, 0.12D, 0.01D);
        } else if (target.entity().filter(entity -> entity == context.player()).isPresent()) {
            arrow.shootFromRotation(context.player(), context.player().getXRot(), context.player().getYRot() + (context.castIndex() - (context.castCount() - 1) / 2.0F) * 4.0F, 0.0F, 2.4F, 0.6F);
        } else {
            Vec3 direction = target.position().subtract(context.player().getEyePosition());
            if (direction.lengthSqr() < 0.001D) {
                direction = context.player().getLookAngle();
            }
            arrow.shoot(direction.x, direction.y, direction.z, 2.4F, 0.6F);
        }
        context.level().addFreshEntity(arrow);
        if (context.castIndex() == 0) {
            context.beam(context.player().getEyePosition().add(context.player().getLookAngle().scale(12 * context.data().power())), ParticleTypes.ENCHANTED_HIT);
        }
        return true;
    }

    private static Vec3 overheadTarget(SpellContext context, SpellTarget target) {
        return target.entity().filter(entity -> entity == context.player()).isPresent()
                ? context.player().getEyePosition().add(context.player().getLookAngle().scale(8.0D))
                : target.position();
    }
}
