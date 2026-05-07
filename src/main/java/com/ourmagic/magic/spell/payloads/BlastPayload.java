package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;

import com.ourmagic.magic.Spell;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class BlastPayload implements PayloadEffect {
    private static final double BLAST_SPEED = 0.85D;

    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.EXPLOSION;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_MULTISTRIKE);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        Vec3 targetPos = target.entity().filter(entity -> entity == context.player()).isPresent()
                ? context.player().getEyePosition().add(context.player().getLookAngle().scale(8.0D))
                : target.position();
        Vec3 start = context.areaCast()
                ? context.areaOrigin().orElse(context.player().getEyePosition()).add(0.0D, 0.35D, 0.0D)
                : context.selfShape() ? targetPos.add(0.0D, 8.0D, 0.0D) : context.player().getEyePosition();
        Vec3 direction = targetPos.subtract(start);
        if (direction.lengthSqr() < 0.001D) {
            direction = context.player().getLookAngle();
        }
        direction = direction.normalize();

        LargeFireball blast = new LargeFireball(context.level(), context.player(), direction.x, direction.y, direction.z, 1);
        blast.setPos(start.x, start.y, start.z);
        blast.setDeltaMovement(direction.scale(BLAST_SPEED));
        context.level().addFreshEntity(blast);

        if (context.castIndex() == 0) {
            context.burst(start, ParticleTypes.EXPLOSION, 4, 0.18D, 0.02D);
        }
        return true;
    }
}
