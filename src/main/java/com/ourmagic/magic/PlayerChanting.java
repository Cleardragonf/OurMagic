package com.ourmagic.magic;

import com.ourmagic.OurMagic;
import com.ourmagic.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class PlayerChanting {
    private static final int MAX_LEVEL = 5;
    private static final int CHARGE_TTL_TICKS = 20 * 45;
    private static final Map<UUID, Charge> CHARGES = new ConcurrentHashMap<>();

    private PlayerChanting() {
    }

    public static void start(ServerPlayer player, InteractionHand hand) {
        CHARGES.put(player.getUUID(), new Charge(hand, 0, player.level().getGameTime()));
    }

    public static void update(ServerPlayer player, InteractionHand hand, int level) {
        if (!hasWand(player.getItemInHand(hand))) {
            cancel(player);
            return;
        }

        int clampedLevel = Math.max(0, Math.min(MAX_LEVEL, level));
        if (clampedLevel <= 0) {
            start(player, hand);
            return;
        }
        CHARGES.put(player.getUUID(), new Charge(hand, clampedLevel, player.level().getGameTime()));
    }

    public static void cancel(ServerPlayer player) {
        CHARGES.remove(player.getUUID());
    }

    public static Optional<Bonus> consume(ServerPlayer player, Spell spell) {
        Optional<Bonus> bonus = currentBonus(player, spell);
        if (bonus.isPresent()) {
            CHARGES.remove(player.getUUID());
        }
        return bonus;
    }

    public static Optional<Bonus> currentBonus(ServerPlayer player, Spell spell) {
        Charge charge = CHARGES.get(player.getUUID());
        if (charge == null || charge.level <= 0 || !spell.supportsChanting()) {
            return Optional.empty();
        }
        return Optional.of(new Bonus(charge.level, 1.0F + charge.level * 1.5F, Math.max(0.15F, 1.0F - charge.level * 0.15F)));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        Charge charge = CHARGES.get(player.getUUID());
        if (charge == null) {
            return;
        }
        if (!hasWand(player.getItemInHand(charge.hand)) || level.getGameTime() - charge.startedAt > CHARGE_TTL_TICKS) {
            cancel(player);
            return;
        }

        if (player.tickCount % 3 == 0) {
            emitParticles(level, player, charge);
        }
        if (charge.level >= MAX_LEVEL) {
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 10, 0, true, false, false));
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        CHARGES.remove(event.getEntity().getUUID());
    }

    private static void emitParticles(ServerLevel level, ServerPlayer player, Charge charge) {
        Vec3 look = player.getLookAngle();
        Vec3 side = new Vec3(-look.z, 0.0D, look.x).normalize();
        if (side.lengthSqr() < 0.001D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        }
        double handSide = charge.hand == InteractionHand.MAIN_HAND ? 0.26D : -0.26D;
        Vec3 tip = player.getEyePosition()
                .add(look.scale(0.72D))
                .add(side.scale(handSide))
                .add(0.0D, -0.28D, 0.0D);
        double spread = 0.05D + charge.level * 0.025D;
        int count = Math.max(3, 2 + charge.level * 2);
        level.sendParticles(ParticleTypes.ENCHANT, tip.x, tip.y, tip.z, count, spread, spread, spread, 0.01D);
        level.sendParticles(ParticleTypes.GLOW, tip.x, tip.y, tip.z, Math.max(1, charge.level), spread * 0.7D, spread * 0.7D, spread * 0.7D, 0.0D);
    }

    private static boolean hasWand(ItemStack stack) {
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
    }

    private record Charge(InteractionHand hand, int level, long startedAt) {
    }

    public record Bonus(int level, float powerMultiplier, float manaCostMultiplier) {
    }
}
