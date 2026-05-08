package com.ourmagic.magic.spell.payloads;

import com.ourmagic.OurMagic;
import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public class RunePayload implements PayloadEffect {
    private static final List<PlacedRune> RUNES = new ArrayList<>();
    private static final int LIFETIME_TICKS = 20 * 18;
    private static final double TRIGGER_RADIUS = 1.25D;

    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.ENCHANT;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DURATION, Spell.UPGRADE_RADIUS);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        return place(context, target, List.of());
    }

    boolean place(SpellContext context, SpellTarget target, List<PayloadEffect> triggeredPayloads) {
        if (!(context.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Vec3 position = target.block().map(block -> block.above().getCenter()).orElse(target.position());
        int lifetime = Math.round(LIFETIME_TICKS * context.durationMultiplier());
        RUNES.add(new PlacedRune(serverLevel, position, lifetime, context.withCastIteration(context.castIndex(), context.castCount(), context.modifierPower()), List.copyOf(triggeredPayloads)));
        context.ring(position, ParticleTypes.ENCHANT, 0.85D, 32);
        context.burst(position, ParticleTypes.WITCH, 18, 0.25D, 0.01D);
        return true;
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || RUNES.isEmpty()) {
            return;
        }

        Iterator<PlacedRune> iterator = RUNES.iterator();
        while (iterator.hasNext()) {
            PlacedRune rune = iterator.next();
            if (rune.ticks <= 0) {
                iterator.remove();
                continue;
            }

            rune.ticks--;
            if (rune.level.getGameTime() % 8 == 0) {
                rune.context.ring(rune.position, ParticleTypes.ENCHANT, 0.75D, 18);
            }

            AABB triggerArea = new AABB(rune.position, rune.position).inflate(TRIGGER_RADIUS * rune.context.radiusMultiplier(), 0.8D, TRIGGER_RADIUS * rune.context.radiusMultiplier());
            List<LivingEntity> entities = rune.level.getEntitiesOfClass(LivingEntity.class, triggerArea, entity -> entity.isAlive() && entity != rune.context.player());
            if (entities.isEmpty()) {
                continue;
            }

            LivingEntity trigger = entities.get(0);
            SpellTarget target = SpellTarget.living(trigger);
            rune.context.burst(rune.position, ParticleTypes.FLASH, 1, 0.0D, 0.0D);
            rune.context.burst(rune.position, ParticleTypes.ENCHANT, 45, 0.75D, 0.04D);
            if (rune.payloads.isEmpty()) {
                trigger.hurt(rune.level.damageSources().magic(), 3.0F * rune.context.damagePower());
            } else {
                SpellContext triggerContext = rune.context.withCastIteration(rune.context.castIndex(), rune.context.castCount(), rune.context.modifierPower()).withoutBeams();
                for (PayloadEffect payload : rune.payloads) {
                    payload.apply(triggerContext, target);
                }
            }
            iterator.remove();
        }
    }

    private static final class PlacedRune {
        private final ServerLevel level;
        private final Vec3 position;
        private final SpellContext context;
        private final List<PayloadEffect> payloads;
        private int ticks;

        private PlacedRune(ServerLevel level, Vec3 position, int ticks, SpellContext context, List<PayloadEffect> payloads) {
            this.level = level;
            this.position = position;
            this.ticks = ticks;
            this.context = context;
            this.payloads = payloads;
        }
    }
}
