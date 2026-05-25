package com.ourmagic.magic.spell.payloads;

import com.ourmagic.OurMagic;
import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.magic.spell.runtime.MagicAllies;
import com.ourmagic.magic.spell.runtime.MagicStatusEffects;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.BlockPos;
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
import java.util.Set;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public class WardPayload implements PayloadEffect {
    private static final List<PlacedWard> WARDS = new ArrayList<>();
    private static final int BASE_LIFETIME_TICKS = 20 * 30;
    private static final double BASE_RADIUS = 3.0D;
    private static final int TRIGGER_COOLDOWN_TICKS = 12;

    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.END_ROD;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_DURATION, Spell.UPGRADE_RADIUS);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        return applyProtectiveWard(context, target);
    }

    boolean place(SpellContext context, SpellTarget target, List<PayloadEffect> triggeredPayloads) {
        if (triggeredPayloads.isEmpty()) {
            return applyProtectiveWard(context, target);
        }
        if (!(context.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        Vec3 position = target.block().map(BlockPos::getCenter).orElse(target.position());
        int lifetime = Math.max(20, Math.round(BASE_LIFETIME_TICKS * context.durationMultiplier()));
        double radius = Math.max(1.25D, BASE_RADIUS * context.data().power() * context.radiusMultiplier());
        int charges = Math.max(1, Math.round(context.utilityPower()));
        TargetRule targetRule = TargetRule.fromShape(SpellRegistry.shapeKey(context.spell().key()));
        WARDS.add(new PlacedWard(serverLevel, position, context.withCastIteration(context.castIndex(), context.castCount(), context.modifierPower()).withoutBeams(), List.copyOf(triggeredPayloads), targetRule, lifetime, radius, charges));

        context.ring(position, ParticleTypes.END_ROD, radius, 48);
        context.ring(position.add(0.0D, 0.14D, 0.0D), ParticleTypes.ENCHANT, radius * 0.72D, 34);
        context.burst(position.add(0.0D, 0.35D, 0.0D), ParticleTypes.WITCH, 22, 0.35D, 0.01D);
        return true;
    }

    private boolean applyProtectiveWard(SpellContext context, SpellTarget target) {
        if (target.entity().isEmpty() || !(target.entity().get() instanceof LivingEntity living)) {
            return false;
        }

        int duration = Math.round(20 * 18 * context.durationMultiplier() * context.utilityPower());
        MagicStatusEffects.ward(living, duration, context.utilityPower());
        context.ring(living.position().add(0, living.getBbHeight() * 0.5D, 0), ParticleTypes.END_ROD, Math.max(0.8D, living.getBbWidth() + 0.35D), 34);
        context.burst(living.position().add(0, living.getBbHeight() * 0.75D, 0), ParticleTypes.ENCHANT, 22, 0.45D, 0.02D);
        return true;
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || WARDS.isEmpty()) {
            return;
        }

        Iterator<PlacedWard> iterator = WARDS.iterator();
        while (iterator.hasNext()) {
            PlacedWard ward = iterator.next();
            if (ward.ticks <= 0 || ward.charges <= 0) {
                iterator.remove();
                continue;
            }

            ward.ticks--;
            if (ward.level.getGameTime() % 14 == 0) {
                ward.context.ring(ward.position, ParticleTypes.END_ROD, ward.radius, 28);
            }
            if (ward.triggerCooldown > 0) {
                ward.triggerCooldown--;
                continue;
            }

            AABB triggerArea = new AABB(ward.position, ward.position).inflate(ward.radius, 1.5D, ward.radius);
            List<LivingEntity> candidates = ward.level.getEntitiesOfClass(LivingEntity.class, triggerArea, entity -> entity.isAlive() && ward.targetRule.matches(ward.context.player(), entity));
            if (candidates.isEmpty()) {
                continue;
            }

            LivingEntity trigger = candidates.get(0);
            SpellTarget spellTarget = SpellTarget.living(trigger);
            SpellContext triggerContext = ward.context.asAreaCast(ward.position).withoutBeams();
            ward.context.burst(ward.position.add(0.0D, 0.35D, 0.0D), ParticleTypes.FLASH, 1, 0.0D, 0.0D);
            ward.context.burst(trigger.position().add(0.0D, trigger.getBbHeight() * 0.5D, 0.0D), ParticleTypes.END_ROD, 32, 0.45D, 0.04D);
            for (PayloadEffect payload : ward.payloads) {
                payload.apply(triggerContext, spellTarget);
            }

            ward.charges--;
            ward.triggerCooldown = TRIGGER_COOLDOWN_TICKS;
            if (ward.charges <= 0) {
                iterator.remove();
            }
        }
    }

    private enum TargetRule {
        HOSTILE,
        PLAYERS,
        ALLIES,
        ANY;

        private static TargetRule fromShape(String shape) {
            return switch (shape) {
                case "ward_players" -> PLAYERS;
                case "ward_allies" -> ALLIES;
                case "ward_any" -> ANY;
                default -> HOSTILE;
            };
        }

        private boolean matches(ServerPlayer caster, LivingEntity entity) {
            return switch (this) {
                case HOSTILE -> entity != caster && !MagicAllies.isAlly(caster, entity);
                case PLAYERS -> entity instanceof ServerPlayer player && player != caster && !MagicAllies.isAlly(caster, player);
                case ALLIES -> MagicAllies.isAlly(caster, entity);
                case ANY -> entity != caster;
            };
        }
    }

    private static final class PlacedWard {
        private final ServerLevel level;
        private final Vec3 position;
        private final SpellContext context;
        private final List<PayloadEffect> payloads;
        private final TargetRule targetRule;
        private final double radius;
        private int ticks;
        private int charges;
        private int triggerCooldown;

        private PlacedWard(ServerLevel level, Vec3 position, SpellContext context, List<PayloadEffect> payloads, TargetRule targetRule, int ticks, double radius, int charges) {
            this.level = level;
            this.position = position;
            this.context = context;
            this.payloads = payloads;
            this.targetRule = targetRule;
            this.ticks = ticks;
            this.radius = radius;
            this.charges = charges;
        }
    }
}
