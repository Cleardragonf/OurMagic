package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;

import com.ourmagic.magic.Spell;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class FireballPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.FLAME;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_MULTISTRIKE, Spell.UPGRADE_CHAINING);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        int i = context.castIndex();
        Vec3 start = context.areaCast()
                ? context.areaOrigin().orElse(context.player().getEyePosition()).add(0.0D, 0.35D, 0.0D)
                : context.player().getEyePosition();
        Vec3 targetPos = target.position();
        if (!context.areaCast() && context.selfShape()) {
            targetPos = target.entity().filter(entity -> entity == context.player()).isPresent()
                    ? context.player().getEyePosition().add(context.player().getLookAngle().scale(8.0D))
                    : target.position();
            start = targetPos.add(0.0D, 7.5D, 0.0D);
        }

        Vec3 direction = context.areaCast() || context.selfShape()
                ? targetPos.subtract(start).normalize()
                : target.entity().filter(entity -> entity == context.player()).isPresent()
                        ? context.player().getLookAngle()
                        : target.position().subtract(context.player().getEyePosition()).normalize();
        if (direction.lengthSqr() < 0.001D) {
            direction = context.player().getLookAngle();
        }
        direction = direction.add((context.player().getRandom().nextDouble() - 0.5D) * 0.04D * i, (context.player().getRandom().nextDouble() - 0.5D) * 0.02D * i, (context.player().getRandom().nextDouble() - 0.5D) * 0.04D * i);
        SmallFireball fireball = new SmallFireball(context.level(), context.player(), direction.x, direction.y, direction.z);
        fireball.setPos(start.x, start.y, start.z);
        fireball.setDeltaMovement(fireball.getDeltaMovement().scale(context.data().activeDamageMultiplier() * context.modifierPower()));
        context.level().addFreshEntity(fireball);
        if (i == 0) {
            if (!context.selfShape() && !context.areaCast()) {
                context.beam(context.player().getEyePosition().add(context.player().getLookAngle().scale(8)), ParticleTypes.FLAME);
            }
            context.burst(start, ParticleTypes.LAVA, 8, 0.12D, 0.04D);
        }
        return true;
    }
}
