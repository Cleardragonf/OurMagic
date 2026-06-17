package com.ourmagic.block.entity;

import com.ourmagic.magic.energy.MagicEnergyType;
import com.ourmagic.magic.energy.MagicEnergyReceiver;
import com.ourmagic.registry.ModBlockEntities;
import com.ourmagic.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

public class WardStoneBlockEntity extends BlockEntity implements MagicEnergyReceiver {
    private static final String TAG_MAGIC_FLOW = "MagicFlow";
    private static final String TAG_MAGIC_FLOW_TYPES = "MagicFlowTypes";
    private static final int MAGIC_FLOW_CAPACITY_PER_TYPE = 100_000;
    private static final int MAX_MULTIBLOCK_STONES = 128;
    private static final Comparator<BlockPos> MASTER_ORDER = Comparator
            .comparingInt((BlockPos pos) -> pos.getY())
            .thenComparingInt(pos -> pos.getX())
            .thenComparingInt(pos -> pos.getZ());
    private final int[] magicFlow = new int[MagicEnergyType.values().length];

    public WardStoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WARD_STONE.get(), pos, state);
    }

    public static Optional<WardStoneBlockEntity> getOrCreate(ServerLevel level, BlockPos pos) {
        Optional<WardMultiblock> multiblock = findMultiblock(level, pos);
        if (multiblock.isEmpty()) {
            return Optional.empty();
        }
        WardStoneBlockEntity master = getOrCreateLocal(level, multiblock.get().master()).orElse(null);
        if (master == null) {
            return Optional.empty();
        }
        consolidateMagicFlow(level, multiblock.get(), master);
        return Optional.of(master);
    }

    public static Optional<WardMultiblock> findMultiblock(ServerLevel level, BlockPos start) {
        if (!level.getBlockState(start).is(ModBlocks.WARD_STONE.get())) {
            return Optional.empty();
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        Set<Long> stones = new HashSet<>();
        BlockPos master = start.immutable();
        queue.add(start.immutable());

        while (!queue.isEmpty() && stones.size() < MAX_MULTIBLOCK_STONES) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos.asLong()) || !level.getBlockState(pos).is(ModBlocks.WARD_STONE.get())) {
                continue;
            }

            getOrCreateLocal(level, pos);
            stones.add(pos.asLong());
            if (MASTER_ORDER.compare(pos, master) < 0) {
                master = pos.immutable();
            }

            for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (!visited.contains(neighbor.asLong()) && level.getBlockState(neighbor).is(ModBlocks.WARD_STONE.get())) {
                    queue.add(neighbor.immutable());
                }
            }
        }

        return Optional.of(new WardMultiblock(master.immutable(), Set.copyOf(stones)));
    }

    private static Optional<WardStoneBlockEntity> getOrCreateLocal(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof WardStoneBlockEntity wardStone) {
            return Optional.of(wardStone);
        }
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.WARD_STONE.get())) {
            return Optional.empty();
        }
        WardStoneBlockEntity wardStone = new WardStoneBlockEntity(pos, state);
        level.setBlockEntity(wardStone);
        return Optional.of(wardStone);
    }

    public int magicFlow() {
        int total = 0;
        for (MagicEnergyType type : MagicEnergyType.values()) {
            total += magicFlow(type);
        }
        return total;
    }

    public int magicFlow(MagicEnergyType type) {
        WardStoneBlockEntity master = masterOrSelf();
        return master == this ? magicFlow[type.ordinal()] : master.magicFlow(type);
    }

    public int magicFlowCapacity() {
        return magicFlowCapacityPerType() * MagicEnergyType.values().length;
    }

    public int magicFlowCapacity(MagicEnergyType type) {
        return magicFlowCapacityPerType();
    }

    public int magicFlowCapacityPerType() {
        return MAGIC_FLOW_CAPACITY_PER_TYPE;
    }

    public int multiblockSize() {
        if (level instanceof ServerLevel serverLevel) {
            return findMultiblock(serverLevel, worldPosition).map(WardMultiblock::size).orElse(1);
        }
        return 1;
    }

    public int wardSlots() {
        return multiblockSize();
    }

    public boolean hasMagicFlow(int amount) {
        return hasMagicFlow(MagicEnergyType.ARCANE, amount);
    }

    public boolean hasMagicFlow(MagicEnergyType type, int amount) {
        return magicFlow(type) >= amount;
    }

    @Override
    public int receiveMagicEnergy(MagicEnergyType type, int amount, boolean simulate) {
        return receiveMagicFlow(type, amount, simulate);
    }

    public int receiveMagicFlow(int amount, boolean simulate) {
        return receiveMagicFlow(MagicEnergyType.ARCANE, amount, simulate);
    }

    public int receiveMagicFlow(MagicEnergyType type, int amount, boolean simulate) {
        WardStoneBlockEntity master = masterOrSelf();
        if (master != this) {
            return master.receiveMagicFlow(type, amount, simulate);
        }
        if (amount <= 0) {
            return 0;
        }
        int index = type.ordinal();
        int accepted = Math.min(amount, magicFlowCapacity(type) - magicFlow[index]);
        if (accepted > 0 && !simulate) {
            magicFlow[index] += accepted;
            setChangedAndUpdate();
        }
        return accepted;
    }

    public boolean consumeMagicFlow(int amount, boolean simulate) {
        return consumeMagicFlow(MagicEnergyType.ARCANE, amount, simulate);
    }

    public boolean consumeMagicFlow(MagicEnergyType type, int amount, boolean simulate) {
        WardStoneBlockEntity master = masterOrSelf();
        if (master != this) {
            return master.consumeMagicFlow(type, amount, simulate);
        }
        if (amount <= 0) {
            return true;
        }
        int index = type.ordinal();
        if (magicFlow[index] < amount) {
            return false;
        }
        if (!simulate) {
            magicFlow[index] -= amount;
            setChangedAndUpdate();
        }
        return true;
    }

    public void setMagicFlow(int amount) {
        setMagicFlow(MagicEnergyType.ARCANE, amount);
    }

    public void setMagicFlow(MagicEnergyType type, int amount) {
        WardStoneBlockEntity master = masterOrSelf();
        if (master != this) {
            master.setMagicFlow(type, amount);
            return;
        }
        magicFlow[type.ordinal()] = Math.max(0, Math.min(magicFlowCapacity(type), amount));
        setChangedAndUpdate();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        java.util.Arrays.fill(magicFlow, 0);
        CompoundTag typed = tag.getCompound(TAG_MAGIC_FLOW_TYPES);
        for (MagicEnergyType type : MagicEnergyType.values()) {
            magicFlow[type.ordinal()] = Math.max(0, Math.min(magicFlowCapacity(type), typed.getInt(type.serializedName())));
        }
        if (tag.contains(TAG_MAGIC_FLOW) && magicFlow() == 0) {
            magicFlow[MagicEnergyType.ARCANE.ordinal()] = Math.max(0, Math.min(magicFlowCapacity(MagicEnergyType.ARCANE), tag.getInt(TAG_MAGIC_FLOW)));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag typed = new CompoundTag();
        for (MagicEnergyType type : MagicEnergyType.values()) {
            int stored = magicFlow[type.ordinal()];
            if (stored > 0) {
                typed.putInt(type.serializedName(), stored);
            }
        }
        tag.put(TAG_MAGIC_FLOW_TYPES, typed);
        tag.putInt(TAG_MAGIC_FLOW, magicFlow());
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

    private void setChangedAndUpdate() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private WardStoneBlockEntity masterOrSelf() {
        if (level instanceof ServerLevel serverLevel) {
            return getOrCreate(serverLevel, worldPosition).orElse(this);
        }
        return this;
    }

    private static void consolidateMagicFlow(ServerLevel level, WardMultiblock multiblock, WardStoneBlockEntity master) {
        int capacity = MAGIC_FLOW_CAPACITY_PER_TYPE;
        long[] totals = new long[MagicEnergyType.values().length];
        for (long stone : multiblock.stones()) {
            Optional<WardStoneBlockEntity> wardStone = getOrCreateLocal(level, BlockPos.of(stone));
            if (wardStone.isEmpty()) {
                continue;
            }
            for (MagicEnergyType type : MagicEnergyType.values()) {
                int index = type.ordinal();
                totals[index] += wardStone.get().magicFlow[index];
                if (!wardStone.get().worldPosition.equals(master.worldPosition) && wardStone.get().magicFlow[index] != 0) {
                    wardStone.get().magicFlow[index] = 0;
                    wardStone.get().setChangedAndUpdate();
                }
            }
        }

        boolean changed = false;
        for (MagicEnergyType type : MagicEnergyType.values()) {
            int index = type.ordinal();
            int consolidated = (int) Math.min(capacity, totals[index]);
            if (master.magicFlow[index] != consolidated) {
                master.magicFlow[index] = consolidated;
                changed = true;
            }
        }
        if (changed) {
            master.setChangedAndUpdate();
        }
    }

    public record WardMultiblock(BlockPos master, Set<Long> stones) {
        public int size() {
            return stones.size();
        }

        public int capacity() {
            return MAGIC_FLOW_CAPACITY_PER_TYPE;
        }

        public boolean contains(BlockPos pos) {
            return stones.contains(pos.asLong());
        }
    }
}
