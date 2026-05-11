package com.ourmagic.magic.spell.runtime;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

import java.util.Optional;
import java.util.UUID;

public final class MagicAllies {
    private static final String TAG_SUMMONED = "OurMagicSummoned";
    private static final String TAG_SUMMON_OWNER = "OurMagicSummonOwner";

    private MagicAllies() {
    }

    public static boolean isAlly(ServerPlayer caster, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        if (entity == caster) {
            return true;
        }
        if (entity instanceof ServerPlayer player) {
            return arePlayersAllied(caster, player);
        }
        return owner(living).map(owner -> isOwnerAllied(caster, owner)).orElse(false);
    }

    public static void markSummonOwner(LivingEntity entity, UUID owner) {
        CompoundTag tag = entity.getPersistentData();
        tag.putBoolean(TAG_SUMMONED, true);
        tag.putUUID(TAG_SUMMON_OWNER, owner);
    }

    public static boolean sameSummonOwner(LivingEntity first, LivingEntity second) {
        if (first == null || second == null) {
            return false;
        }
        Optional<UUID> firstOwner = summonOwner(first);
        return firstOwner.isPresent() && firstOwner.equals(summonOwner(second));
    }

    private static boolean arePlayersAllied(ServerPlayer caster, ServerPlayer player) {
        return caster.getUUID().equals(player.getUUID()) || caster.isAlliedTo(player) || player.isAlliedTo(caster);
    }

    private static boolean isOwnerAllied(ServerPlayer caster, UUID owner) {
        if (caster.getUUID().equals(owner)) {
            return true;
        }
        if (!(caster.level() instanceof ServerLevel level)) {
            return false;
        }
        ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(owner);
        return ownerPlayer != null && arePlayersAllied(caster, ownerPlayer);
    }

    private static Optional<UUID> owner(LivingEntity entity) {
        Optional<UUID> summonOwner = summonOwner(entity);
        if (summonOwner.isPresent()) {
            return summonOwner;
        }
        if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
            return Optional.ofNullable(tamable.getOwnerUUID());
        }
        return Optional.empty();
    }

    private static Optional<UUID> summonOwner(LivingEntity entity) {
        CompoundTag tag = entity.getPersistentData();
        if (!tag.getBoolean(TAG_SUMMONED) || !tag.hasUUID(TAG_SUMMON_OWNER)) {
            return Optional.empty();
        }
        return Optional.of(tag.getUUID(TAG_SUMMON_OWNER));
    }
}
