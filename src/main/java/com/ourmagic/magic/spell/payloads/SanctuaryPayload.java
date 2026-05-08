package com.ourmagic.magic.spell.payloads;

import com.ourmagic.OurMagic;
import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public class SanctuaryPayload implements PayloadEffect {
    private static final List<Sanctuary> SANCTUARIES = new ArrayList<>();

    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.HAPPY_VILLAGER;
    }

    @Override
    public java.util.Set<String> supportedUpgrades(SpellBuildContext context) {
        return java.util.Set.of(Spell.UPGRADE_DURATION, Spell.UPGRADE_RADIUS);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (!(context.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 center = target.position();
        double radius = 4.0D * context.radiusMultiplier();
        SANCTUARIES.add(new Sanctuary(level, center, Math.round(20 * 12 * context.durationMultiplier()), radius, context.utilityPower()));
        context.ring(center, ParticleTypes.HAPPY_VILLAGER, radius, 56);
        context.burst(center, ParticleTypes.TOTEM_OF_UNDYING, 25, 0.75D, 0.04D);
        return true;
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SANCTUARIES.isEmpty()) {
            return;
        }
        Iterator<Sanctuary> iterator = SANCTUARIES.iterator();
        while (iterator.hasNext()) {
            Sanctuary sanctuary = iterator.next();
            if (sanctuary.ticks-- <= 0) {
                iterator.remove();
                continue;
            }
            if (sanctuary.level.getGameTime() % 10 == 0) {
                for (int i = 0; i < 18; i++) {
                    double angle = Math.PI * 2.0D * i / 18.0D;
                    sanctuary.level.sendParticles(ParticleTypes.HAPPY_VILLAGER, sanctuary.center.x + Math.cos(angle) * sanctuary.radius, sanctuary.center.y + 0.2D, sanctuary.center.z + Math.sin(angle) * sanctuary.radius, 1, 0.0D, 0.05D, 0.0D, 0.0D);
                }
            }
            if (sanctuary.level.getGameTime() % 20 == 0) {
                AABB area = new AABB(sanctuary.center, sanctuary.center).inflate(sanctuary.radius);
                for (LivingEntity living : sanctuary.level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
                    if (living instanceof ServerPlayer) {
                        living.heal(1.5F * sanctuary.power);
                    } else {
                        Vec3 push = living.position().subtract(sanctuary.center).normalize().scale(0.45D);
                        living.push(push.x, 0.08D, push.z);
                        living.hurtMarked = true;
                    }
                }
            }
        }
    }

    private static final class Sanctuary {
        private final ServerLevel level;
        private final Vec3 center;
        private final double radius;
        private final float power;
        private int ticks;

        private Sanctuary(ServerLevel level, Vec3 center, int ticks, double radius, float power) {
            this.level = level;
            this.center = center;
            this.ticks = ticks;
            this.radius = radius;
            this.power = power;
        }
    }
}
