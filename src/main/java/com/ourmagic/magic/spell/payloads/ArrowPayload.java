package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;

import com.ourmagic.magic.Spell;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class ArrowPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.ENCHANTED_HIT;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DAMAGE, Spell.UPGRADE_MULTISTRIKE);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        Arrow arrow = new Arrow(context.level(), context.player());
        arrow.setBaseDamage(3.0D * context.damagePower());
        if (context.areaCast()) {
            Vec3 targetPos = target.position();
            Vec3 start = context.areaOrigin().orElse(context.player().getEyePosition());
            if (start.distanceToSqr(targetPos) < 0.001D) {
                start = start.add(0.0D, 1.0D, 0.0D);
            }
            start = start.add((context.castIndex() - (context.castCount() - 1) / 2.0F) * 0.25D, 0.35D, 0.0D);
            arrow.setPos(start.x, start.y, start.z);
            arrow.shoot(targetPos.x - start.x, targetPos.y - start.y, targetPos.z - start.z, 2.6F, 0.15F);
            context.burst(start, ParticleTypes.ENCHANTED_HIT, 6, 0.12D, 0.01D);
        } else if (context.selfShape()) {
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
