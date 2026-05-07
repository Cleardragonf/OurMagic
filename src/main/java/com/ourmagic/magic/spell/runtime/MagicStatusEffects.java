package com.ourmagic.magic.spell.runtime;

import com.ourmagic.OurMagic;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class MagicStatusEffects {
    private static final Map<UUID, BoundState> BOUND = new HashMap<>();
    private static final Map<UUID, StunState> STUNNED = new HashMap<>();
    private static final Map<UUID, WarpState> WARPED = new HashMap<>();

    private MagicStatusEffects() {
    }

    public static void bind(LivingEntity entity, int ticks) {
        BOUND.put(entity.getUUID(), new BoundState(ticks, entity.position()));
        entity.setDeltaMovement(Vec3.ZERO);
    }

    public static void stun(ServerPlayer player, int ticks) {
        STUNNED.put(player.getUUID(), new StunState(ticks, player.position()));
        player.setDeltaMovement(Vec3.ZERO);
        player.setPose(Pose.SLEEPING);
    }

    public static void warp(ServerPlayer player, int ticks) {
        WarpState existing = WARPED.get(player.getUUID());
        WARPED.put(player.getUUID(), new WarpState(Math.max(ticks, existing == null ? 0 : existing.ticks()), existing == null ? player.gameMode.getGameModeForPlayer() : existing.previousMode()));
        player.setGameMode(GameType.SPECTATOR);
    }

    public static void nullify(LivingEntity entity) {
        BOUND.remove(entity.getUUID());
        wake(entity);
        WarpState warp = WARPED.remove(entity.getUUID());
        if (warp != null && entity instanceof ServerPlayer player) {
            player.setGameMode(warp.previousMode());
        }

        entity.removeEffect(MobEffects.BLINDNESS);
        entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        entity.removeEffect(MobEffects.LEVITATION);
        entity.removeEffect(MobEffects.WATER_BREATHING);
        entity.removeEffect(MobEffects.REGENERATION);
        entity.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }

    public static boolean isBound(Entity entity) {
        return BOUND.containsKey(entity.getUUID());
    }

    public static void wake(LivingEntity entity) {
        if (STUNNED.remove(entity.getUUID()) != null && entity instanceof ServerPlayer player) {
            player.setPose(Pose.STANDING);
        }
    }

    @SubscribeEvent
    public static void livingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        BoundState bound = BOUND.get(entity.getUUID());
        if (bound == null) {
            return;
        }

        entity.setDeltaMovement(Vec3.ZERO);
        entity.teleportTo(bound.anchor().x, bound.anchor().y, bound.anchor().z);
        if (entity.level() instanceof ServerLevel serverLevel && entity.tickCount % 10 == 0) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5D, entity.getZ(), 8, 0.35D, 0.35D, 0.35D, 0.01D);
        }
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        StunState stun = STUNNED.get(player.getUUID());
        if (stun != null) {
            player.setDeltaMovement(Vec3.ZERO);
            player.teleportTo(stun.anchor().x, stun.anchor().y, stun.anchor().z);
            player.setPose(Pose.SLEEPING);
            if (stun.ticks() <= 1) {
                STUNNED.remove(player.getUUID());
                player.setPose(Pose.STANDING);
            } else {
                STUNNED.put(player.getUUID(), new StunState(stun.ticks() - 1, stun.anchor()));
            }
            if (player.level() instanceof ServerLevel serverLevel && player.tickCount % 10 == 0) {
                serverLevel.sendParticles(ParticleTypes.WITCH, player.getX(), player.getY() + 0.8D, player.getZ(), 6, 0.25D, 0.2D, 0.25D, 0.01D);
            }
        }

        WarpState warp = WARPED.get(player.getUUID());
        if (warp != null) {
            if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                WARPED.remove(player.getUUID());
                return;
            }
            if (warp.ticks() <= 1) {
                WARPED.remove(player.getUUID());
                player.setGameMode(warp.previousMode());
            } else {
                WARPED.put(player.getUUID(), new WarpState(warp.ticks() - 1, warp.previousMode()));
            }
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        tickBound();
    }

    @SubscribeEvent
    public static void teleport(EntityTeleportEvent event) {
        if (isBound(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void livingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() != null) {
            wake(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void entityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof ServerPlayer player && STUNNED.containsKey(player.getUUID())) {
            wake(player);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            wake(player);
            WarpState warp = WARPED.remove(player.getUUID());
            if (warp != null) {
                player.setGameMode(warp.previousMode());
            }
        }
    }

    private static void tickBound() {
        Iterator<Map.Entry<UUID, BoundState>> iterator = BOUND.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BoundState> entry = iterator.next();
            BoundState state = entry.getValue();
            if (state.ticks() <= 1) {
                iterator.remove();
            } else {
                entry.setValue(new BoundState(state.ticks() - 1, state.anchor()));
            }
        }
    }

    private record BoundState(int ticks, Vec3 anchor) {
    }

    private record StunState(int ticks, Vec3 anchor) {
    }

    private record WarpState(int ticks, GameType previousMode) {
    }
}
