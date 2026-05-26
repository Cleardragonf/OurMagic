package com.ourmagic.magic.spell.runtime;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class MagicAllies {
    private static final String TAG_ALLIES = "OurMagicAllies";
    private static final String TAG_ALLY_ID = "Id";
    private static final String TAG_ALLY_NAME = "Name";
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

    public static boolean isAlly(ServerLevel level, UUID owner, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return false;
        }
        if (entity.getUUID().equals(owner)) {
            return true;
        }
        ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(owner);
        if (ownerPlayer != null && isAlly(ownerPlayer, entity)) {
            return true;
        }
        return owner(living).map(livingOwner -> livingOwner.equals(owner)).orElse(false);
    }

    public static boolean addAlly(ServerPlayer owner, ServerPlayer ally) {
        if (owner.getUUID().equals(ally.getUUID()) || hasListedAlly(owner, ally.getUUID())) {
            return false;
        }
        CompoundTag entry = new CompoundTag();
        entry.putUUID(TAG_ALLY_ID, ally.getUUID());
        entry.putString(TAG_ALLY_NAME, ally.getGameProfile().getName());
        allyTags(owner).add(entry);
        return true;
    }

    public static boolean removeAlly(ServerPlayer owner, ServerPlayer ally) {
        ListTag allies = allyTags(owner);
        for (int i = 0; i < allies.size(); i++) {
            CompoundTag entry = allies.getCompound(i);
            if (entry.hasUUID(TAG_ALLY_ID) && entry.getUUID(TAG_ALLY_ID).equals(ally.getUUID())) {
                allies.remove(i);
                return true;
            }
        }
        return false;
    }

    public static int clearAllies(ServerPlayer owner) {
        ListTag allies = allyTags(owner);
        int count = allies.size();
        allies.clear();
        return count;
    }

    public static List<AllyEntry> allies(ServerPlayer owner) {
        List<AllyEntry> entries = new ArrayList<>();
        ListTag allies = allyTags(owner);
        for (int i = 0; i < allies.size(); i++) {
            CompoundTag entry = allies.getCompound(i);
            if (entry.hasUUID(TAG_ALLY_ID)) {
                String name = entry.contains(TAG_ALLY_NAME, Tag.TAG_STRING) ? entry.getString(TAG_ALLY_NAME) : entry.getUUID(TAG_ALLY_ID).toString();
                entries.add(new AllyEntry(entry.getUUID(TAG_ALLY_ID), name));
            }
        }
        return List.copyOf(entries);
    }

    public static void copy(Player original, Player target) {
        CompoundTag originalData = original.getPersistentData();
        if (originalData.contains(TAG_ALLIES, Tag.TAG_LIST)) {
            target.getPersistentData().put(TAG_ALLIES, originalData.getList(TAG_ALLIES, Tag.TAG_COMPOUND).copy());
        }
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

    public static boolean summonOwnerIsAlly(LivingEntity summon, Entity entity) {
        Optional<UUID> owner = summonOwner(summon);
        return owner.isPresent()
                && summon.level() instanceof ServerLevel level
                && isAlly(level, owner.get(), entity);
    }

    private static boolean arePlayersAllied(ServerPlayer caster, ServerPlayer player) {
        return caster.getUUID().equals(player.getUUID())
                || caster.isAlliedTo(player)
                || player.isAlliedTo(caster)
                || hasListedAlly(caster, player.getUUID());
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

    private static boolean hasListedAlly(ServerPlayer owner, UUID ally) {
        ListTag allies = allyTags(owner);
        for (int i = 0; i < allies.size(); i++) {
            CompoundTag entry = allies.getCompound(i);
            if (entry.hasUUID(TAG_ALLY_ID) && entry.getUUID(TAG_ALLY_ID).equals(ally)) {
                return true;
            }
        }
        return false;
    }

    private static ListTag allyTags(ServerPlayer owner) {
        CompoundTag data = owner.getPersistentData();
        if (!data.contains(TAG_ALLIES, Tag.TAG_LIST)) {
            data.put(TAG_ALLIES, new ListTag());
        }
        return data.getList(TAG_ALLIES, Tag.TAG_COMPOUND);
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

    public record AllyEntry(UUID id, String name) {
    }
}
