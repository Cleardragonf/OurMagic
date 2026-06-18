package com.ourmagic.block.entity;

import com.ourmagic.magic.energy.MagicEnergyReceiver;
import com.ourmagic.magic.energy.MagicEnergyStorage;
import com.ourmagic.magic.energy.MagicEnergyType;
import com.ourmagic.magic.energy.MagicTransferParticles;
import com.ourmagic.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MagicRelayBlockEntity extends BlockEntity implements MagicEnergyReceiver {
    private static final String TAG_ENERGY = "MagicEnergy";
    private static final String TAG_LINKS = "MagicLinks";
    private static final int CAPACITY = 100_000;
    private static final int PUSH_PER_TYPE_PER_TICK = 480;
    private static final int MAX_LINKS = 16;
    private static final int MAX_LINK_DISTANCE = 32;

    private final MagicEnergyStorage energy = new MagicEnergyStorage(CAPACITY);
    private final java.util.Set<Long> linkedTargets = new java.util.LinkedHashSet<>();
    private int lastPushed;

    public MagicRelayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGIC_RELAY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MagicRelayBlockEntity relay) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        relay.lastPushed = relay.pushToReceivers(serverLevel, pos);
        if (relay.lastPushed > 0) {
            relay.setChangedAndUpdate();
        }
    }

    @Override
    public int receiveMagicEnergy(MagicEnergyType type, int amount, boolean simulate) {
        int accepted = energy.receive(type, amount, simulate);
        if (accepted > 0 && !simulate) {
            setChangedAndUpdate();
        }
        return accepted;
    }

    public int stored(MagicEnergyType type) {
        return energy.stored(type);
    }

    public int capacity() {
        return energy.capacity();
    }

    public int lastPushed() {
        return lastPushed;
    }

    public int linkCount() {
        return linkedTargets.size();
    }

    public int inboundLinkCount() {
        return level instanceof ServerLevel serverLevel ? MagicLinkNetwork.inboundLinkCount(serverLevel, worldPosition) : 0;
    }

    public int totalLinkCount(ServerLevel level) {
        return linkedTargets.size() + MagicLinkNetwork.inboundLinkCount(level, worldPosition);
    }

    public boolean linksTo(BlockPos target) {
        return linkedTargets.contains(target.asLong());
    }

    public int maxLinks() {
        return MAX_LINKS;
    }

    public LinkResult toggleLink(BlockPos target) {
        if (target.equals(worldPosition)) {
            return new LinkResult(false, "Cannot link a relay to itself.");
        }
        if (worldPosition.distSqr(target) > MAX_LINK_DISTANCE * MAX_LINK_DISTANCE) {
            return new LinkResult(false, "Target is too far away. Max range is " + MAX_LINK_DISTANCE + " blocks.");
        }
        if (!(level instanceof ServerLevel serverLevel) || magicReceiver(serverLevel, target).isEmpty()) {
            return new LinkResult(false, "That block cannot receive magic energy.");
        }
        long key = target.asLong();
        if (linkedTargets.remove(key)) {
            setChangedAndUpdate();
            return new LinkResult(true, "Magic relay link removed: " + target.toShortString() + ".");
        }
        if (totalLinkCount(serverLevel) >= MAX_LINKS) {
            return new LinkResult(false, "This relay already has " + MAX_LINKS + " links.");
        }
        if (!MagicLinkNetwork.targetHasLinkCapacity(serverLevel, target)) {
            return new LinkResult(false, "Target already has its maximum links.");
        }
        linkedTargets.add(key);
        setChangedAndUpdate();
        return new LinkResult(true, "Magic relay link added: " + target.toShortString() + ".");
    }

    public String statusLine() {
        StringBuilder builder = new StringBuilder();
        int inboundLinks = inboundLinkCount();
        builder.append("Links: ").append(linkedTargets.size() + inboundLinks).append("/").append(MAX_LINKS)
                .append(" (in ").append(inboundLinks).append(", out ").append(linkedTargets.size()).append(")")
                .append(". Last push: ").append(lastPushed).append(" ME/t. ");
        for (MagicEnergyType type : MagicEnergyType.values()) {
            if (builder.charAt(builder.length() - 1) != ' ') {
                builder.append(", ");
            }
            builder.append(type.displayName()).append(" ").append(stored(type)).append("/").append(capacity());
        }
        return builder.append(".").toString();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TAG_ENERGY)) {
            energy.load(tag.getCompound(TAG_ENERGY));
        }
        linkedTargets.clear();
        ListTag links = tag.getList(TAG_LINKS, net.minecraft.nbt.Tag.TAG_LONG);
        for (int i = 0; i < links.size(); i++) {
            linkedTargets.add(((LongTag) links.get(i)).getAsLong());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag energyTag = new CompoundTag();
        energy.save(energyTag);
        tag.put(TAG_ENERGY, energyTag);
        ListTag links = new ListTag();
        for (long linkedTarget : linkedTargets) {
            links.add(LongTag.valueOf(linkedTarget));
        }
        tag.put(TAG_LINKS, links);
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

    private int pushToReceivers(ServerLevel level, BlockPos pos) {
        if (linkedTargets.isEmpty()) {
            return 0;
        }

        List<TransferTarget> targets = new ArrayList<>();
        java.util.Iterator<Long> iterator = linkedTargets.iterator();
        boolean removedInvalidLink = false;
        while (iterator.hasNext()) {
            BlockPos targetPos = BlockPos.of(iterator.next());
            Optional<MagicEnergyReceiver> receiver = magicReceiver(level, targetPos);
            if (receiver.isEmpty()) {
                iterator.remove();
                removedInvalidLink = true;
                continue;
            }
            targets.add(new TransferTarget(targetPos.immutable(), receiver.get()));
        }
        if (removedInvalidLink) {
            setChangedAndUpdate();
        }
        if (targets.isEmpty()) {
            return 0;
        }

        int pushed = 0;
        for (MagicEnergyType type : MagicEnergyType.values()) {
            int budget = Math.min(PUSH_PER_TYPE_PER_TICK, stored(type));
            int remainingTargets = targets.size();
            for (TransferTarget target : targets) {
                if (budget <= 0 || remainingTargets <= 0) {
                    break;
                }
                int share = Math.max(1, (int) Math.ceil(budget / (double) remainingTargets));
                int sent = pushToReceiver(level, pos, target.pos(), target.receiver(), type, Math.min(share, stored(type)));
                pushed += sent;
                budget -= sent;
                remainingTargets--;
            }
        }
        return pushed;
    }

    private int pushToReceiver(Level level, BlockPos sourcePos, BlockPos targetPos, MagicEnergyReceiver receiver, MagicEnergyType type, int amount) {
        int accepted = receiver.receiveMagicEnergy(type, amount, true);
        if (accepted <= 0) {
            return 0;
        }
        int extracted = energy.extract(type, accepted, false);
        int received = receiver.receiveMagicEnergy(type, extracted, false);
        if (received < extracted) {
            energy.receive(type, extracted - received, false);
        }
        MagicTransferParticles.emit(level, sourcePos, targetPos, type, received);
        return received;
    }

    private static Optional<MagicEnergyReceiver> magicReceiver(ServerLevel level, BlockPos pos) {
        BlockEntity target = level.getBlockEntity(pos);
        if (target instanceof MagicEnergyReceiver receiver) {
            return Optional.of(receiver);
        }
        return WardStoneBlockEntity.getOrCreate(level, pos).map(wardStone -> (MagicEnergyReceiver) wardStone);
    }

    private void setChangedAndUpdate() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public record LinkResult(boolean success, String message) {
    }

    private record TransferTarget(BlockPos pos, MagicEnergyReceiver receiver) {
    }
}
