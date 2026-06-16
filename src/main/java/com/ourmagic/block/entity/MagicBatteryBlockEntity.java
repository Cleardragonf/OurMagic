package com.ourmagic.block.entity;

import com.ourmagic.magic.energy.MagicEnergyReceiver;
import com.ourmagic.magic.energy.MagicEnergyType;
import com.ourmagic.registry.ModBlockEntities;
import com.ourmagic.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

public class MagicBatteryBlockEntity extends BlockEntity implements MagicEnergyReceiver {
    private static final String TAG_MAGIC_ENERGY = "MagicEnergy";
    private static final int MAGIC_ENERGY_CAPACITY_PER_BLOCK = 500_000;
    private static final int MAX_MULTIBLOCK_BLOCKS = 64;
    private static final Comparator<BlockPos> MASTER_ORDER = Comparator
            .comparingInt((BlockPos pos) -> pos.getY())
            .thenComparingInt(pos -> pos.getX())
            .thenComparingInt(pos -> pos.getZ());

    private final int[] energy = new int[MagicEnergyType.values().length];

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
}
