package com.ourmagic.block.entity;

import com.ourmagic.registry.ModBlockEntities;
import com.ourmagic.registry.ModBlocks;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.network.TeleportPortalDataPacket;
import com.ourmagic.magic.spell.runtime.MagicAllies;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraftforge.network.PacketDistributor;

public class TeleportPortalBlockEntity extends BlockEntity {
    private static final String TAG_TARGET = "TargetPortal";
    private static final String TAG_NAME = "PortalName";
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_TARGET_OWNER = "TargetOwner";
    private static final String TAG_BEAM_TICK = "BeamTick";
    private static final String TAG_BEAM_DIRECTION = "BeamDirection";
    private static final double PORTAL_RADIUS = 2.0D;
    private static final int COOLDOWN_TICKS = 60;
    private static final Map<UUID, Long> NEXT_TELEPORT_TICK = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<BlockPos, TeleportPortalBlockEntity>> LOADED_PORTALS = new HashMap<>();
    private BlockPos targetPortal;
    private String portalName = "";
    private UUID owner;
    private UUID targetOwner;
    private long beamTick = -100L;
    private int beamDirection;

    public TeleportPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TELEPORT_PORTAL.get(), pos, state);
    }

    public Optional<BlockPos> targetPortal() {
        return Optional.ofNullable(targetPortal);
    }

    public String portalName() {
        return portalName.isBlank() ? defaultName(worldPosition) : portalName;
    }

    public void openManagement(ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        List<TeleportPortalDataPacket.Entry> entries = loadedPortals(serverLevel).stream()
                .filter(portal -> !portal.getBlockPos().equals(worldPosition))
                .filter(portal -> portal.canManage(serverLevel, player))
                .map(portal -> new TeleportPortalDataPacket.Entry(portal.getBlockPos(), portal.portalName(), portal.statusLine()))
                .sorted(Comparator.comparing(TeleportPortalDataPacket.Entry::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new TeleportPortalDataPacket(worldPosition, portalName(), Optional.ofNullable(targetPortal), entries));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TeleportPortalBlockEntity portal) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (portal.targetPortal == null) {
            return;
        }
        AABB bounds = new AABB(
                pos.getX() + 0.5D - PORTAL_RADIUS, pos.getY() - 0.05D, pos.getZ() + 0.5D - PORTAL_RADIUS,
                pos.getX() + 0.5D + PORTAL_RADIUS, pos.getY() + 2.25D, pos.getZ() + 0.5D + PORTAL_RADIUS);
        for (Entity entity : level.getEntities((Entity) null, bounds, entity -> entity.isAlive() && isInsidePortalRadius(entity, pos))) {
            portal.tryTeleport(serverLevel, entity);
        }
    }

    public LinkResult toggleLink(ServerLevel level, BlockPos target) {
        if (target.equals(worldPosition)) {
            return new LinkResult(false, "Cannot link a portal to itself.");
        }
        if (!level.getBlockState(target).is(ModBlocks.TELEPORT_PORTAL.get())) {
            return new LinkResult(false, "Teleportation Portals link to other Teleportation Portals.");
        }
        if (target.equals(targetPortal)) {
            clearLink();
            portalAt(level, target).ifPresent(other -> {
                if (worldPosition.equals(other.targetPortal)) {
                    other.clearLink();
                }
            });
            return new LinkResult(true, "Teleportation portal link removed.");
        }

        targetPortal = target.immutable();
        portalAt(level, target).ifPresent(other -> targetOwner = other.owner);
        setChangedAndUpdate();
        portalAt(level, target).ifPresent(other -> {
            other.setLink(worldPosition);
            other.targetOwner = owner;
            other.setChangedAndUpdate();
        });
        return new LinkResult(true, "Teleportation portal linked to " + target.toShortString() + ".");
    }

    public LinkResult setManagedLink(ServerLevel level, ServerPlayer actor, Optional<BlockPos> target, String name) {
        setPortalName(name);
        if (target.isEmpty()) {
            clearManagedLink(level);
            return new LinkResult(true, "Teleportation portal updated.");
        }
        BlockPos targetPos = target.get();
        if (targetPos.equals(worldPosition)) {
            return new LinkResult(false, "Cannot link a portal to itself.");
        }
        Optional<TeleportPortalBlockEntity> otherPortal = portalAt(level, targetPos);
        if (otherPortal.isEmpty()) {
            return new LinkResult(false, "Selected portal is not loaded.");
        }
        if (!otherPortal.get().canManage(level, actor)) {
            return new LinkResult(false, "Selected portal cannot be connected by this player.");
        }
        clearManagedLink(level);
        targetPortal = targetPos.immutable();
        targetOwner = otherPortal.get().owner;
        setChangedAndUpdate();
        otherPortal.get().clearManagedLink(level);
        otherPortal.get().setLink(worldPosition);
        otherPortal.get().targetOwner = owner;
        otherPortal.get().setChangedAndUpdate();
        return new LinkResult(true, "Teleportation portal updated.");
    }

    public void tryTeleport(ServerLevel level, Entity entity) {
        if (targetPortal == null || !level.getBlockState(targetPortal).is(ModBlocks.TELEPORT_PORTAL.get())) {
            return;
        }
        long gameTime = level.getGameTime();
        if (NEXT_TELEPORT_TICK.getOrDefault(entity.getUUID(), 0L) > gameTime) {
            return;
        }
        NEXT_TELEPORT_TICK.put(entity.getUUID(), gameTime + COOLDOWN_TICKS);
        triggerBeam(level, 1);
        portalAt(level, targetPortal).ifPresent(other -> other.triggerBeam(level, -1));
        double x = targetPortal.getX() + 0.5D;
        double y = targetPortal.getY() + 0.12D;
        double z = targetPortal.getZ() + 0.5D;
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
        } else {
            entity.teleportTo(x, y, z);
        }
        entity.resetFallDistance();
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                worldPosition.getX() + 0.5D, worldPosition.getY() + 0.15D, worldPosition.getZ() + 0.5D,
                35, 0.55D, 0.05D, 0.55D, 0.08D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                targetPortal.getX() + 0.5D, targetPortal.getY() + 0.15D, targetPortal.getZ() + 0.5D,
                35, 0.55D, 0.05D, 0.55D, 0.08D);
    }

    public long beamTick() {
        return beamTick;
    }

    public int beamDirection() {
        return beamDirection;
    }

    public String statusLine() {
        return targetPortal == null ? "Unlinked." : "Linked to " + targetPortal.toShortString() + ".";
    }

    public void setOwner(ServerPlayer player) {
        if (owner == null) {
            owner = player.getUUID();
            setChangedAndUpdate();
        }
    }

    public boolean canManage(ServerLevel level, ServerPlayer player) {
        return owner == null
                || player.getUUID().equals(owner)
                || player.getUUID().equals(targetOwner)
                || MagicAllies.isAlly(level, owner, player);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        registerLoadedPortal();
    }

    @Override
    public void setRemoved() {
        unregisterLoadedPortal();
        super.setRemoved();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        targetPortal = tag.contains(TAG_TARGET) ? BlockPos.of(tag.getLong(TAG_TARGET)) : null;
        portalName = tag.getString(TAG_NAME);
        owner = tag.hasUUID(TAG_OWNER) ? tag.getUUID(TAG_OWNER) : null;
        targetOwner = tag.hasUUID(TAG_TARGET_OWNER) ? tag.getUUID(TAG_TARGET_OWNER) : null;
        beamTick = tag.contains(TAG_BEAM_TICK) ? tag.getLong(TAG_BEAM_TICK) : -100L;
        beamDirection = tag.getInt(TAG_BEAM_DIRECTION);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (targetPortal != null) {
            tag.putLong(TAG_TARGET, targetPortal.asLong());
        }
        if (!portalName.isBlank()) {
            tag.putString(TAG_NAME, portalName);
        }
        if (owner != null) {
            tag.putUUID(TAG_OWNER, owner);
        }
        if (targetOwner != null) {
            tag.putUUID(TAG_TARGET_OWNER, targetOwner);
        }
        tag.putLong(TAG_BEAM_TICK, beamTick);
        tag.putInt(TAG_BEAM_DIRECTION, beamDirection);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void setLink(BlockPos target) {
        targetPortal = target.immutable();
        setChangedAndUpdate();
    }

    private void clearLink() {
        targetPortal = null;
        targetOwner = null;
        setChangedAndUpdate();
    }

    private void clearManagedLink(ServerLevel level) {
        BlockPos previous = targetPortal;
        targetPortal = null;
        targetOwner = null;
        setChangedAndUpdate();
        if (previous != null) {
            portalAt(level, previous).ifPresent(other -> {
                if (worldPosition.equals(other.targetPortal)) {
                    other.clearLink();
                }
            });
        }
    }

    private void setPortalName(String name) {
        portalName = sanitizeName(name);
        setChangedAndUpdate();
    }

    private static Optional<TeleportPortalBlockEntity> portalAt(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof TeleportPortalBlockEntity portal ? Optional.of(portal) : Optional.empty();
    }

    public static Optional<TeleportPortalBlockEntity> managedPortalAt(ServerLevel level, BlockPos pos) {
        return portalAt(level, pos);
    }

    private static List<TeleportPortalBlockEntity> loadedPortals(ServerLevel level) {
        Map<BlockPos, TeleportPortalBlockEntity> portals = LOADED_PORTALS.get(level.dimension());
        if (portals == null || portals.isEmpty()) {
            return List.of();
        }
        List<TeleportPortalBlockEntity> valid = new ArrayList<>();
        portals.entrySet().removeIf(entry -> entry.getValue().isRemoved()
                || entry.getValue().level != level
                || !level.getBlockState(entry.getKey()).is(ModBlocks.TELEPORT_PORTAL.get()));
        for (TeleportPortalBlockEntity portal : portals.values()) {
            valid.add(portal);
        }
        return valid;
    }

    private void registerLoadedPortal() {
        if (level == null || level.isClientSide) {
            return;
        }
        LOADED_PORTALS.computeIfAbsent(level.dimension(), key -> new HashMap<>()).put(worldPosition, this);
    }

    private void unregisterLoadedPortal() {
        if (level == null || level.isClientSide) {
            return;
        }
        Map<BlockPos, TeleportPortalBlockEntity> portals = LOADED_PORTALS.get(level.dimension());
        if (portals != null) {
            portals.remove(worldPosition);
        }
    }

    private void triggerBeam(ServerLevel level, int direction) {
        beamTick = level.getGameTime();
        beamDirection = direction;
        setChangedAndUpdate();
    }

    private static boolean isInsidePortalRadius(Entity entity, BlockPos pos) {
        double dx = entity.getX() - (pos.getX() + 0.5D);
        double dz = entity.getZ() - (pos.getZ() + 0.5D);
        return dx * dx + dz * dz <= PORTAL_RADIUS * PORTAL_RADIUS;
    }

    private static String sanitizeName(String name) {
        String trimmed = name == null ? "" : name.trim();
        return trimmed.length() > 40 ? trimmed.substring(0, 40) : trimmed;
    }

    private static String defaultName(BlockPos pos) {
        return "Portal " + pos.toShortString();
    }

    private void setChangedAndUpdate() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public record LinkResult(boolean success, String message) {
    }
}
