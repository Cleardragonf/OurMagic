package com.ourmagic.block.entity;

import com.ourmagic.block.MagicAccumulatorBlock;
import com.ourmagic.magic.energy.MagicEnergyReceiver;
import com.ourmagic.magic.energy.MagicEnergyStorage;
import com.ourmagic.magic.energy.MagicEnergyType;
import com.ourmagic.magic.energy.MagicTransferParticles;
import com.ourmagic.magic.energy.MagicTransferMode;
import com.ourmagic.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class MagicAccumulatorBlockEntity extends BlockEntity implements MagicEnergyReceiver {
    private static final String TAG_ENERGY = "MagicEnergy";
    private static final String TAG_LINKS = "MagicLinks";
    private static final String TAG_MODE = "TransferMode";
    private static final int CAPACITY = 20_000;
    private static final int HARVEST_INTERVAL_TICKS = 20;
    private static final int HARVEST_RADIUS = 3;
    private static final int MAX_HARVEST_PER_CYCLE = 250;
    private static final int PUSH_PER_SIDE_PER_TICK = 120;
    private static final int MAX_LINKS = 8;
    private static final int MAX_LINK_DISTANCE = 32;

    private final MagicEnergyStorage energy = new MagicEnergyStorage(CAPACITY);
    private final java.util.Set<Long> linkedTargets = new java.util.LinkedHashSet<>();
    private MagicTransferMode transferMode = MagicTransferMode.OUTPUT;
    private int tickOffset;
    private int lastHarvest;
    private int lastPushed;

    public MagicAccumulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGIC_ACCUMULATOR.get(), pos, state);
        tickOffset = Math.floorMod(pos.getX() * 31 + pos.getY() * 17 + pos.getZ() * 13, HARVEST_INTERVAL_TICKS);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MagicAccumulatorBlockEntity accumulator) {
        accumulator.lastPushed = accumulator.transferMode.canOutput() ? accumulator.pushToReceivers(level, pos) : 0;
        if (Math.floorMod((int) level.getGameTime() + accumulator.tickOffset, HARVEST_INTERVAL_TICKS) == 0) {
            accumulator.lastHarvest = accumulator.harvest(level, pos);
        }
        if (accumulator.lastPushed > 0 || accumulator.lastHarvest > 0) {
            accumulator.setChangedAndUpdate();
        }
    }

    @Override
    public int receiveMagicEnergy(MagicEnergyType type, int amount, boolean simulate) {
        if (!transferMode.canInput() || type != magicType()) {
            return 0;
        }
        int accepted = energy.receive(type, amount, simulate);
        if (accepted > 0 && !simulate) {
            setChangedAndUpdate();
        }
        return accepted;
    }

    public int extractMagicEnergy(MagicEnergyType type, int amount, boolean simulate) {
        int extracted = energy.extract(type, amount, simulate);
        if (extracted > 0 && !simulate) {
            setChangedAndUpdate();
        }
        return extracted;
    }

    public int stored() {
        return energy.stored(magicType());
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

    public int maxLinks() {
        return MAX_LINKS;
    }

    public MagicEnergyType magicType() {
        return blockMagicType();
    }

    public String statusLine() {
        return magicType().displayName() + " ME " + stored() + "/" + capacity()
                + ". Mode: " + transferMode.displayName() + ". Links: " + linkedTargets.size() + "/" + MAX_LINKS
                + ". Last harvest: " + lastHarvest + ", last push: " + lastPushed + ".";
    }

    public MagicTransferMode transferMode() {
        return transferMode;
    }

    public void cycleTransferMode() {
        transferMode = transferMode.next();
        setChangedAndUpdate();
    }

    public LinkResult toggleLink(BlockPos target) {
        if (target.equals(worldPosition)) {
            return new LinkResult(false, "Cannot link an accumulator to itself.");
        }
        if (worldPosition.distSqr(target) > MAX_LINK_DISTANCE * MAX_LINK_DISTANCE) {
            return new LinkResult(false, "Target is too far away. Max range is " + MAX_LINK_DISTANCE + " blocks.");
        }
        long key = target.asLong();
        if (linkedTargets.remove(key)) {
            setChangedAndUpdate();
            return new LinkResult(true, "Magic link removed: " + target.toShortString() + ".");
        }
        if (linkedTargets.size() >= MAX_LINKS) {
            return new LinkResult(false, "This accumulator already has " + MAX_LINKS + " links.");
        }
        linkedTargets.add(key);
        setChangedAndUpdate();
        return new LinkResult(true, "Magic link added: " + target.toShortString() + ".");
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
        if (tag.contains(TAG_MODE)) {
            try {
                transferMode = MagicTransferMode.valueOf(tag.getString(TAG_MODE));
            } catch (IllegalArgumentException ignored) {
                transferMode = MagicTransferMode.OUTPUT;
            }
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
            links.add(net.minecraft.nbt.LongTag.valueOf(linkedTarget));
        }
        tag.put(TAG_LINKS, links);
        tag.putString(TAG_MODE, transferMode.name());
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

    private int harvest(Level level, BlockPos pos) {
        MagicEnergyType type = magicType();
        int generated = 0;
        BlockPos.MutableBlockPos scanPos = new BlockPos.MutableBlockPos();
        for (int x = -HARVEST_RADIUS; x <= HARVEST_RADIUS && generated < MAX_HARVEST_PER_CYCLE; x++) {
            for (int y = -HARVEST_RADIUS; y <= HARVEST_RADIUS && generated < MAX_HARVEST_PER_CYCLE; y++) {
                for (int z = -HARVEST_RADIUS; z <= HARVEST_RADIUS && generated < MAX_HARVEST_PER_CYCLE; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    scanPos.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    generated += energy.receive(type, Math.min(type.sourceValue(level, scanPos), MAX_HARVEST_PER_CYCLE - generated), false);
                }
            }
        }
        return generated;
    }

    private int pushToReceivers(Level level, BlockPos pos) {
        MagicEnergyType type = magicType();
        java.util.List<TransferTarget> targets = new java.util.ArrayList<>();
        java.util.Set<Long> seenTargets = new java.util.LinkedHashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos targetPos = pos.relative(direction);
            BlockEntity neighbor = level.getBlockEntity(targetPos);
            if (!(neighbor instanceof MagicEnergyReceiver receiver)) {
                continue;
            }
            if (seenTargets.add(targetPos.asLong())) {
                targets.add(new TransferTarget(targetPos.immutable(), receiver));
            }
        }

        java.util.Iterator<Long> iterator = linkedTargets.iterator();
        boolean removedInvalidLink = false;
        while (iterator.hasNext()) {
            BlockPos targetPos = BlockPos.of(iterator.next());
            BlockEntity target = level.getBlockEntity(targetPos);
            if (!(target instanceof MagicEnergyReceiver receiver)) {
                iterator.remove();
                removedInvalidLink = true;
                continue;
            }
            if (seenTargets.add(targetPos.asLong())) {
                targets.add(new TransferTarget(targetPos.immutable(), receiver));
            }
        }
        if (removedInvalidLink) {
            setChangedAndUpdate();
        }

        int pushed = 0;
        int budget = Math.min(PUSH_PER_SIDE_PER_TICK, stored());
        int remainingTargets = targets.size();
        for (TransferTarget target : targets) {
            if (budget <= 0 || stored() <= 0 || remainingTargets <= 0) {
                break;
            }
            int share = Math.max(1, (int) Math.ceil(budget / (double) remainingTargets));
            int sent = pushToReceiver(level, pos, target.pos(), target.receiver(), type, Math.min(share, stored()));
            pushed += sent;
            budget -= sent;
            remainingTargets--;
        }
        return pushed;
    }

    private int pushToReceiver(Level level, BlockPos sourcePos, BlockPos targetPos, MagicEnergyReceiver receiver, MagicEnergyType type, int amount) {
        int accepted = receiver.receiveMagicEnergy(type, amount, true);
        if (accepted <= 0) {
            return 0;
        }
        int extracted = extractMagicEnergy(type, accepted, false);
        int received = receiver.receiveMagicEnergy(type, extracted, false);
        if (received < extracted) {
            energy.receive(type, extracted - received, false);
        }
        MagicTransferParticles.emit(level, sourcePos, targetPos, type, received);
        return received;
    }

    private MagicEnergyType blockMagicType() {
        if (getBlockState().getBlock() instanceof MagicAccumulatorBlock accumulatorBlock) {
            return accumulatorBlock.magicType();
        }
        return MagicEnergyType.ARCANE;
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
