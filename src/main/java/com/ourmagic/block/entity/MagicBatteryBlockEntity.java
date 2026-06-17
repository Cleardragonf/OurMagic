package com.ourmagic.block.entity;

import com.ourmagic.magic.energy.MagicEnergyReceiver;
import com.ourmagic.magic.energy.MagicEnergyType;
import com.ourmagic.registry.ModBlockEntities;
import com.ourmagic.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class MagicBatteryBlockEntity extends BlockEntity implements MagicEnergyReceiver {
    private static final String TAG_MAGIC_ENERGY = "MagicEnergy";
    private static final String TAG_LINKS = "MagicLinks";
    private static final int MAGIC_ENERGY_CAPACITY_PER_BLOCK = 500_000;
    private static final int MAX_MULTIBLOCK_BLOCKS = 64;
    private static final int MAX_LINKS = 8;
    private static final int MAX_LINK_DISTANCE = 32;
    private static final int PUSH_PER_TYPE_PER_TICK = 240;
    private static final Comparator<BlockPos> MASTER_ORDER = Comparator
            .comparingInt((BlockPos pos) -> pos.getY())
            .thenComparingInt(pos -> pos.getX())
            .thenComparingInt(pos -> pos.getZ());

    private final int[] energy = new int[MagicEnergyType.values().length];
    private final Set<Long> linkedTargets = new java.util.LinkedHashSet<>();

    public MagicBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGIC_BATTERY.get(), pos, state);
    }

    public static Optional<MagicBatteryBlockEntity> getOrCreate(ServerLevel level, BlockPos pos) {
        Optional<BatteryMultiblock> multiblock = findMultiblock(level, pos);
        if (multiblock.isEmpty()) {
            return Optional.empty();
        }
        MagicBatteryBlockEntity master = getOrCreateLocal(level, multiblock.get().master()).orElse(null);
        if (master == null) {
            return Optional.empty();
        }
        consolidateEnergy(level, multiblock.get(), master);
        return Optional.of(master);
    }

    public static Optional<BatteryMultiblock> findMultiblock(ServerLevel level, BlockPos start) {
        if (!level.getBlockState(start).is(ModBlocks.MAGIC_BATTERY.get())) {
            return Optional.empty();
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        Set<Long> blocks = new HashSet<>();
        BlockPos master = start.immutable();
        queue.add(start.immutable());

        while (!queue.isEmpty() && blocks.size() < MAX_MULTIBLOCK_BLOCKS) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos.asLong()) || !level.getBlockState(pos).is(ModBlocks.MAGIC_BATTERY.get())) {
                continue;
            }

            getOrCreateLocal(level, pos);
            blocks.add(pos.asLong());
            if (MASTER_ORDER.compare(pos, master) < 0) {
                master = pos.immutable();
            }

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (!visited.contains(neighbor.asLong()) && level.getBlockState(neighbor).is(ModBlocks.MAGIC_BATTERY.get())) {
                    queue.add(neighbor.immutable());
                }
            }
        }

        return Optional.of(new BatteryMultiblock(master.immutable(), Set.copyOf(blocks)));
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, MagicBatteryBlockEntity battery) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        MagicBatteryBlockEntity master = getOrCreate(serverLevel, pos).orElse(null);
        if (master == null || !battery.getBlockPos().equals(master.getBlockPos())) {
            return;
        }
        int pushed = master.pushToReceivers(serverLevel);
        if (pushed > 0) {
            master.setChangedAndUpdate();
        }
    }

    private static Optional<MagicBatteryBlockEntity> getOrCreateLocal(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MagicBatteryBlockEntity battery) {
            return Optional.of(battery);
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.MAGIC_BATTERY.get())) {
            return Optional.empty();
        }
        MagicBatteryBlockEntity battery = new MagicBatteryBlockEntity(pos, state);
        level.setBlockEntity(battery);
        return Optional.of(battery);
    }

    @Override
    public int receiveMagicEnergy(MagicEnergyType type, int amount, boolean simulate) {
        MagicBatteryBlockEntity master = masterOrSelf();
        if (master != this) {
            return master.receiveMagicEnergy(type, amount, simulate);
        }
        if (amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, capacity() - storedLocal(type));
        if (accepted > 0 && !simulate) {
            energy[type.ordinal()] += accepted;
            setChangedAndUpdate();
        }
        return accepted;
    }

    public int extractMagicEnergy(MagicEnergyType type, int amount, boolean simulate) {
        MagicBatteryBlockEntity master = masterOrSelf();
        if (master != this) {
            return master.extractMagicEnergy(type, amount, simulate);
        }
        if (amount <= 0) {
            return 0;
        }
        int extracted = Math.min(amount, storedLocal(type));
        if (extracted > 0 && !simulate) {
            energy[type.ordinal()] -= extracted;
            setChangedAndUpdate();
        }
        return extracted;
    }

    public int stored(MagicEnergyType type) {
        MagicBatteryBlockEntity master = masterOrSelf();
        return master == this ? storedLocal(type) : master.stored(type);
    }

    public int capacity() {
        return MAGIC_ENERGY_CAPACITY_PER_BLOCK * multiblockSize();
    }

    public int linkCount() {
        return masterOrSelf().linkedTargets.size();
    }

    public int maxLinks() {
        return MAX_LINKS;
    }

    public LinkResult toggleLink(BlockPos target) {
        MagicBatteryBlockEntity master = masterOrSelf();
        if (target.equals(master.worldPosition)) {
            return new LinkResult(false, "Cannot link a battery to itself.");
        }
        if (master.worldPosition.distSqr(target) > MAX_LINK_DISTANCE * MAX_LINK_DISTANCE) {
            return new LinkResult(false, "Target is too far away. Max range is " + MAX_LINK_DISTANCE + " blocks.");
        }
        if (!(level instanceof ServerLevel serverLevel) || magicReceiver(serverLevel, target).isEmpty()) {
            return new LinkResult(false, "That block cannot receive magic energy.");
        }
        long key = target.asLong();
        if (master.linkedTargets.remove(key)) {
            master.setChangedAndUpdate();
            return new LinkResult(true, "Magic battery link removed: " + target.toShortString() + ".");
        }
        if (master.linkedTargets.size() >= MAX_LINKS) {
            return new LinkResult(false, "This battery already has " + MAX_LINKS + " links.");
        }
        master.linkedTargets.add(key);
        master.setChangedAndUpdate();
        return new LinkResult(true, "Magic battery link added: " + target.toShortString() + ".");
    }

    public int multiblockSize() {
        if (level instanceof ServerLevel serverLevel) {
            return findMultiblock(serverLevel, worldPosition).map(BatteryMultiblock::size).orElse(1);
        }
        return 1;
    }

    public String statusLine() {
        StringBuilder builder = new StringBuilder();
        builder.append(multiblockSize()).append(" blocks, ").append(capacity()).append(" ME/type. ");
        for (MagicEnergyType type : MagicEnergyType.values()) {
            if (builder.charAt(builder.length() - 1) != ' ') {
                builder.append(", ");
            }
            builder.append(type.displayName()).append(" ").append(stored(type));
        }
        return builder.append(".").toString();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        CompoundTag energyTag = tag.getCompound(TAG_MAGIC_ENERGY);
        for (MagicEnergyType type : MagicEnergyType.values()) {
            energy[type.ordinal()] = Math.max(0, energyTag.getInt(type.serializedName()));
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
        for (MagicEnergyType type : MagicEnergyType.values()) {
            int stored = storedLocal(type);
            if (stored > 0) {
                energyTag.putInt(type.serializedName(), stored);
            }
        }
        tag.put(TAG_MAGIC_ENERGY, energyTag);
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

    private int storedLocal(MagicEnergyType type) {
        return energy[type.ordinal()];
    }

    private MagicBatteryBlockEntity masterOrSelf() {
        if (level instanceof ServerLevel serverLevel) {
            return getOrCreate(serverLevel, worldPosition).orElse(this);
        }
        return this;
    }

    private void setChangedAndUpdate() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private int pushToReceivers(ServerLevel level) {
        if (linkedTargets.isEmpty()) {
            return 0;
        }
        int pushed = 0;
        java.util.Iterator<Long> iterator = linkedTargets.iterator();
        boolean removedInvalid = false;
        java.util.List<TransferTarget> targets = new java.util.ArrayList<>();
        while (iterator.hasNext()) {
            BlockPos targetPos = BlockPos.of(iterator.next());
            Optional<MagicEnergyReceiver> receiver = magicReceiver(level, targetPos);
            if (receiver.isEmpty()) {
                iterator.remove();
                removedInvalid = true;
                continue;
            }
            targets.add(new TransferTarget(targetPos.immutable(), receiver.get()));
        }
        if (removedInvalid) {
            setChangedAndUpdate();
        }
        if (targets.isEmpty()) {
            return 0;
        }

        for (MagicEnergyType type : MagicEnergyType.values()) {
            int budget = Math.min(PUSH_PER_TYPE_PER_TICK, stored(type));
            int remainingTargets = targets.size();
            for (TransferTarget target : targets) {
                if (budget <= 0 || remainingTargets <= 0) {
                    break;
                }
                int share = Math.max(1, (int) Math.ceil(budget / (double) remainingTargets));
                int accepted = target.receiver().receiveMagicEnergy(type, Math.min(share, stored(type)), true);
                if (accepted > 0) {
                    int extracted = extractMagicEnergy(type, accepted, false);
                    int received = target.receiver().receiveMagicEnergy(type, extracted, false);
                    if (received < extracted) {
                        receiveMagicEnergy(type, extracted - received, false);
                    }
                    pushed += received;
                    budget -= received;
                }
                remainingTargets--;
            }
        }
        return pushed;
    }

    private static Optional<MagicEnergyReceiver> magicReceiver(ServerLevel level, BlockPos pos) {
        BlockEntity target = level.getBlockEntity(pos);
        if (target instanceof MagicEnergyReceiver receiver) {
            return Optional.of(receiver);
        }
        return WardStoneBlockEntity.getOrCreate(level, pos).map(wardStone -> (MagicEnergyReceiver) wardStone);
    }

    private static void consolidateEnergy(ServerLevel level, BatteryMultiblock multiblock, MagicBatteryBlockEntity master) {
        int capacity = MAGIC_ENERGY_CAPACITY_PER_BLOCK * multiblock.size();
        int[] totals = new int[MagicEnergyType.values().length];
        for (long block : multiblock.blocks()) {
            Optional<MagicBatteryBlockEntity> battery = getOrCreateLocal(level, BlockPos.of(block));
            if (battery.isEmpty()) {
                continue;
            }
            for (MagicEnergyType type : MagicEnergyType.values()) {
                totals[type.ordinal()] = Math.min(capacity, totals[type.ordinal()] + battery.get().energy[type.ordinal()]);
                if (!battery.get().worldPosition.equals(master.worldPosition) && battery.get().energy[type.ordinal()] != 0) {
                    battery.get().energy[type.ordinal()] = 0;
                    battery.get().setChangedAndUpdate();
                }
            }
        }

        for (MagicEnergyType type : MagicEnergyType.values()) {
            if (master.energy[type.ordinal()] != totals[type.ordinal()]) {
                master.energy[type.ordinal()] = totals[type.ordinal()];
                master.setChangedAndUpdate();
            }
        }
    }

    public record BatteryMultiblock(BlockPos master, Set<Long> blocks) {
        public int size() {
            return blocks.size();
        }
    }

    public record LinkResult(boolean success, String message) {
    }

    private record TransferTarget(BlockPos pos, MagicEnergyReceiver receiver) {
    }
}
