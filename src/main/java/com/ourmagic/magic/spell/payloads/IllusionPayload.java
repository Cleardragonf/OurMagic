package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class IllusionPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.POOF;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DURATION, Spell.UPGRADE_RADIUS);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        Vec3 center = target.entity().map(entity -> entity.position()).orElse(target.position());
        double radius = 5.0D * context.radiusMultiplier();
        int duration = Math.round(90 * context.durationMultiplier() * context.utilityPower());
        boolean applied = false;
        for (Mob mob : context.level().getEntitiesOfClass(Mob.class, new AABB(center, center).inflate(radius), mob -> mob.isAlive())) {
            mob.setTarget(null);
            mob.addEffect(new MobEffectInstance(MobEffects.CONFUSION, duration, 0));
            applied = true;
        }

        context.burst(center, ParticleTypes.POOF, 45, Math.min(1.5D, radius * 0.25D), 0.05D);
        context.burst(center, ParticleTypes.WITCH, 18, Math.min(1.1D, radius * 0.18D), 0.02D);
        return applied;
    }
}
