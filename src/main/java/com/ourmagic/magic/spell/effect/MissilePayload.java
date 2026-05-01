package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.Vec3;

public class MissilePayload implements PayloadEffect {
    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        ThrownTrident missile = new ThrownTrident(context.level(), context.player(), context.wand().copy());
        missile.setBaseDamage(8.0D * context.damagePower());
        if (target.entity().filter(entity -> entity == context.player()).isPresent()) {
            missile.shootFromRotation(context.player(), context.player().getXRot(), context.player().getYRot() + (context.castIndex() - (context.castCount() - 1) / 2.0F) * 4.0F, 0, 1.6F * context.data().power(), 0.5F);
        } else {
            Vec3 start = context.player().getEyePosition();
            Vec3 direction = target.position().subtract(start);
            if (direction.lengthSqr() < 0.001D) {
                direction = context.player().getLookAngle();
            }
            missile.shoot(direction.x, direction.y, direction.z, 1.6F * context.data().power(), 0.5F);
        }
        context.level().addFreshEntity(missile);
        if (context.castIndex() == 0) {
            context.beam(context.player().getEyePosition().add(context.player().getLookAngle().scale(10 * context.data().power())), ParticleTypes.CRIT);
            context.beam(context.player().getEyePosition().add(context.player().getLookAngle().scale(10 * context.data().power())), ParticleTypes.WITCH);
        }
        return true;
    }
}
