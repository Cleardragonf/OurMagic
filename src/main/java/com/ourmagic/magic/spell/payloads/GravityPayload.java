package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class GravityPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.REVERSE_PORTAL;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_RADIUS);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        Vec3 center = target.position();
        double radius = 5.0D * context.radiusMultiplier();
        boolean applied = false;
        for (LivingEntity living : context.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), entity -> entity != context.player() && entity.isAlive())) {
            Vec3 pull = center.subtract(living.position()).normalize().scale(0.9D * context.utilityPower());
            living.push(pull.x, Math.max(0.08D, pull.y + 0.15D), pull.z);
            living.hurtMarked = true;
            applied = true;
        }

        context.ring(center, ParticleTypes.REVERSE_PORTAL, radius * 0.45D, 48);
        context.burst(center, ParticleTypes.PORTAL, 45, Math.min(1.2D, radius * 0.2D), 0.08D);
        return applied;
    }
}
