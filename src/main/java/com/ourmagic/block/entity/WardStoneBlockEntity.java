package com.ourmagic.block.entity;

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

public class WardStoneBlockEntity extends BlockEntity {
    private static final String TAG_MAGIC_FLOW = "MagicFlow";
    private static final int MAGIC_FLOW_CAPACITY_PER_STONE = 100_000;
    private static final int MAX_MULTIBLOCK_STONES = 128;
    private static final Comparator<BlockPos> MASTER_ORDER = Comparator
            .comparingInt((BlockPos pos) -> pos.getY())
            .thenComparingInt(pos -> pos.getX())
            .thenComparingInt(pos -> pos.getZ());
    private int magicFlow;

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
        WardStoneBlockEntity master = masterOrSelf();
        return master == this ? magicFlow : master.magicFlow();
    }

    public int magicFlowCapacity() {
        return MAGIC_FLOW_CAPACITY_PER_STONE * multiblockSize();
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
        return magicFlow() >= amount;
    }

    public int receiveMagicFlow(int amount, boolean simulate) {
        WardStoneBlockEntity master = masterOrSelf();
        if (master != this) {
            return master.receiveMagicFlow(amount, simulate);
        }
        if (amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, magicFlowCapacity() - magicFlow);
        if (accepted > 0 && !simulate) {
            magicFlow += accepted;
            setChangedAndUpdate();
        }
        return accepted;
    }

    public boolean consumeMagicFlow(int amount, boolean simulate) {
        WardStoneBlockEntity master = masterOrSelf();
        if (master != this) {
            return master.consumeMagicFlow(amount, simulate);
        }
        if (amount <= 0) {
            return true;
        }
        if (magicFlow < amount) {
            return false;
        }
        if (!simulate) {
            magicFlow -= amount;
            setChangedAndUpdate();
        }
        return true;
    }

    public void setMagicFlow(int amount) {
        WardStoneBlockEntity master = masterOrSelf();
        if (master != this) {
            master.setMagicFlow(amount);
            return;
        }
        magicFlow = Math.max(0, Math.min(magicFlowCapacity(), amount));
        setChangedAndUpdate();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        magicFlow = Math.max(0, tag.getInt(TAG_MAGIC_FLOW));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_MAGIC_FLOW, magicFlow);
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
        int capacity = MAGIC_FLOW_CAPACITY_PER_STONE * multiblock.size();
        long total = 0L;
        for (long stone : multiblock.stones()) {
            Optional<WardStoneBlockEntity> wardStone = getOrCreateLocal(level, BlockPos.of(stone));
            if (wardStone.isEmpty()) {
                continue;
            }
            total += wardStone.get().magicFlow;
            if (!wardStone.get().worldPosition.equals(master.worldPosition) && wardStone.get().magicFlow != 0) {
                wardStone.get().magicFlow = 0;
                wardStone.get().setChangedAndUpdate();
            }
        }

        int consolidated = (int) Math.min(capacity, total);
        if (master.magicFlow != consolidated) {
            master.magicFlow = consolidated;
            master.setChangedAndUpdate();
        }
    }

    public record WardMultiblock(BlockPos master, Set<Long> stones) {
        public int size() {
            return stones.size();
        }

        public int capacity() {
            return size() * MAGIC_FLOW_CAPACITY_PER_STONE;
        }

        public boolean contains(BlockPos pos) {
            return stones.contains(pos.asLong());
        }
    }
}
