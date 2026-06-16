package com.ourmagic.event;

import com.ourmagic.OurMagic;
import com.ourmagic.entity.WarlockEntity;
import com.ourmagic.registry.ModEntities;
import com.ourmagic.util.PlayerTitles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class WarlockHuntEvents {
    private static final int CHECK_INTERVAL_TICKS = 20 * 60 * 10;
    private static final int HUNT_COOLDOWN_TICKS = 20 * 60 * 60;
    private static final int MIN_DISTANCE = 28;
    private static final int MAX_DISTANCE = 48;
    private static final int SPAWN_ATTEMPTS = 16;
    private static final float HUNT_CHANCE = 0.35F;
    private static final Map<UUID, Long> NEXT_HUNT_TIME = new HashMap<>();

    private WarlockHuntEvents() {
    }

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || !isEligible(player, level)) {
            return;
        }

        long gameTime = level.getGameTime();
        if (NEXT_HUNT_TIME.getOrDefault(player.getUUID(), 0L) > gameTime || player.getRandom().nextFloat() > HUNT_CHANCE) {
            return;
        }

        if (startHunt(level, player)) {
            NEXT_HUNT_TIME.put(player.getUUID(), gameTime + HUNT_COOLDOWN_TICKS);
        }
    }

    private static boolean isEligible(ServerPlayer player, ServerLevel level) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        return level.dimension() == ServerLevel.OVERWORLD
                && level.getDifficulty() != Difficulty.PEACEFUL
                && mode != GameType.CREATIVE
                && mode != GameType.SPECTATOR
                && !player.isSleeping()
                && player.isAlive();
    }

    private static boolean startHunt(ServerLevel level, ServerPlayer target) {
        BlockPos anchor = findSpawnAnchor(level, target);
        if (anchor == null) {
            return false;
        }

        int count = huntSize(level);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            BlockPos pos = findNearbySpawn(level, anchor);
            if (pos == null) {
                continue;
            }
            WarlockEntity warlock = ModEntities.WARLOCK.get().create(level);
            if (warlock == null) {
                continue;
            }
            warlock.moveTo(pos, level.random.nextFloat() * 360.0F, 0.0F);
            warlock.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            warlock.setTarget(target);
            if (level.addFreshEntity(warlock)) {
                spawned++;
            }
        }

        if (spawned <= 0) {
            return false;
        }

        PlayerTitles.show(target,
                Component.literal("Warlock Hunt").withStyle(ChatFormatting.DARK_PURPLE),
                Component.literal("Hostile magic is tracking you.").withStyle(ChatFormatting.LIGHT_PURPLE),
                10, 45, 15);
        level.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.8F, 0.45F);
        return true;
    }

    private static int huntSize(ServerLevel level) {
        return switch (level.getDifficulty()) {
            case HARD -> 3;
            case NORMAL -> 2;
            default -> 1;
        };
    }

    private static BlockPos findSpawnAnchor(ServerLevel level, ServerPlayer target) {
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int distance = MIN_DISTANCE + level.random.nextInt(MAX_DISTANCE - MIN_DISTANCE + 1);
            int x = target.blockPosition().getX() + Math.round((float) Math.cos(angle) * distance);
            int z = target.blockPosition().getZ() + Math.round((float) Math.sin(angle) * distance);
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, target.blockPosition().getY(), z));
            if (isValidSpawn(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static BlockPos findNearbySpawn(ServerLevel level, BlockPos anchor) {
        for (int attempt = 0; attempt < 6; attempt++) {
            BlockPos pos = anchor.offset(level.random.nextInt(9) - 4, 0, level.random.nextInt(9) - 4);
            pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
            if (isValidSpawn(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static boolean isValidSpawn(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos)
                && level.getWorldBorder().isWithinBounds(pos)
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                && level.getBlockState(pos).isAir()
                && level.getBlockState(pos.above()).isAir()
                && level.noCollision(ModEntities.WARLOCK.get().getAABB(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
    }
}
