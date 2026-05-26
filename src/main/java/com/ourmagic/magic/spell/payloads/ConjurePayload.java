package com.ourmagic.magic.spell.payloads;

import com.ourmagic.OurMagic;
import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.MagicDamageSources;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public class ConjurePayload implements PayloadEffect {
    private static final List<ConjuredWisp> WISPS = new ArrayList<>();

    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.SOUL_FIRE_FLAME;
    }

    @Override
    public java.util.Set<String> supportedUpgrades(SpellBuildContext context) {
        return java.util.Set.of(Spell.UPGRADE_DURATION, Spell.UPGRADE_DAMAGE);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        if (!(context.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 position = target.position();
        WISPS.add(new ConjuredWisp(level, position, Math.round(20 * 14 * context.durationMultiplier()), context.damagePower(), context.player().getUUID()));
        context.ring(position, ParticleTypes.SOUL_FIRE_FLAME, 1.0D, 32);
        context.burst(position, ParticleTypes.SOUL_FIRE_FLAME, 35, 0.45D, 0.05D);
        return true;
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || WISPS.isEmpty()) {
            return;
        }
        Iterator<ConjuredWisp> iterator = WISPS.iterator();
        while (iterator.hasNext()) {
            ConjuredWisp wisp = iterator.next();
            if (wisp.ticks-- <= 0) {
                iterator.remove();
                continue;
            }
            if (wisp.level.getGameTime() % 4 == 0) {
                wisp.level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, wisp.position.x, wisp.position.y + 0.6D, wisp.position.z, 4, 0.2D, 0.2D, 0.2D, 0.01D);
            }
            if (wisp.level.getGameTime() % 20 == 0) {
                for (LivingEntity living : wisp.level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(wisp.position, wisp.position).inflate(3.0D), living -> living.isAlive() && !living.getUUID().equals(wisp.owner))) {
                    living.hurt(MagicDamageSources.playerMagicOrGeneric(wisp.level, wisp.owner), 2.0F * wisp.power);
                    break;
                }
            }
        }
    }

    private static final class ConjuredWisp {
        private final ServerLevel level;
        private final Vec3 position;
        private final float power;
        private final UUID owner;
        private int ticks;

        private ConjuredWisp(ServerLevel level, Vec3 position, int ticks, float power, UUID owner) {
            this.level = level;
            this.position = position;
            this.ticks = ticks;
            this.power = power;
            this.owner = owner;
        }
    }
}
