package com.ourmagic.magic.spell.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;

public class ExplosionPayload implements PayloadEffect {
    private final float strength;
    private final ExplosionInteraction interaction;

    public ExplosionPayload() {
        this(4.0F, ExplosionInteraction.TNT);
    }

    public ExplosionPayload(float strength, ExplosionInteraction interaction) {
        this.strength = strength;
        this.interaction = interaction;
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        Vec3 at = spreadTarget(context, target.position());
        float explosionStrength = strength * context.damagePower() * context.radiusMultiplier();
        context.burst(at, ParticleTypes.EXPLOSION, Math.max(4, Math.round(explosionStrength)), 0.35D * context.radiusMultiplier(), 0.0D);
        context.burst(at, ParticleTypes.FLASH, 1, 0.0D, 0.0D);
        context.level().explode(context.player(), at.x, at.y, at.z, explosionStrength, interaction);
        return true;
    }

    private static Vec3 spreadTarget(SpellContext context, Vec3 target) {
        if (context.castIndex() == 0) {
            return target;
        }
        double spread = 0.7D + context.castIndex() * 0.2D;
        return target.add((context.player().getRandom().nextDouble() - 0.5D) * spread, 0, (context.player().getRandom().nextDouble() - 0.5D) * spread);
    }
}
