package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;

import com.ourmagic.magic.Spell;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class LightningPayload implements PayloadEffect {
    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.ELECTRIC_SPARK;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_CHAINING, Spell.UPGRADE_MULTISTRIKE);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        Vec3 at = spreadTarget(context, target.position());
        if (context.castIndex() == 0) {
            if (context.areaCast()) {
                Vec3 start = target.position().add(0.0D, 7.0D, 0.0D);
                drawDownStrike(context, start, target.position());
            } else {
                context.beam(target.position(), ParticleTypes.ELECTRIC_SPARK);
            }
            context.burst(target.position(), ParticleTypes.ELECTRIC_SPARK, 60, 0.9D, 0.12D);
            context.burst(target.position(), ParticleTypes.FLASH, 1, 0.0D, 0.0D);
        }
        strikeLightning(context, at);
        return true;
    }

    private static void strikeLightning(SpellContext context, Vec3 at) {
        if (!(context.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(context.level());
        if (bolt == null) {
            return;
        }
        bolt.moveTo(at);
        bolt.setCause(context.player());
        serverLevel.addFreshEntity(bolt);
    }

    private static void drawDownStrike(SpellContext context, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        int steps = Math.max(8, (int) (delta.length() * 2.0D));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = start.add(delta.scale(i / (double) steps));
            context.burst(at, ParticleTypes.ELECTRIC_SPARK, 1, 0.04D, 0.0D);
        }
    }

    private static Vec3 spreadTarget(SpellContext context, Vec3 target) {
        if (context.castIndex() == 0) {
            return target;
        }
        double spread = 0.7D + context.castIndex() * 0.2D;
        return target.add((context.player().getRandom().nextDouble() - 0.5D) * spread, 0, (context.player().getRandom().nextDouble() - 0.5D) * spread);
    }
}
