package com.ourmagic.block.entity;

import com.ourmagic.registry.ModBlockEntities;
import com.ourmagic.magic.energy.MagicEnergyReceiver;
import com.ourmagic.magic.energy.MagicEnergyType;
import com.ourmagic.magic.energy.MagicTransferParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

public class MagicFlowConverterBlockEntity extends BlockEntity implements MagicEnergyReceiver {
    private static final int RF_MAX_RECEIVE = 2_000;
    private static final int RF_PER_MAGIC_FLOW = 4;
    private static final int MAGIC_ENERGY_PER_MAGIC_FLOW = 1;

    private final ConverterEnergyStorage energyStorage = new ConverterEnergyStorage();
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);

    public MagicFlowConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGIC_FLOW_CONVERTER.get(), pos, state);
    }

    public int energyStored() {
        return 0;
    }

    public int energyCapacity() {
        return RF_MAX_RECEIVE;
    }

    public int magicFlow() {
        return 0;
    }

    public int magicFlowCapacity() {
        return 0;
    }

    public String linkedWardCoreSummary() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return "No linked Ward Stone core.";
        }
        Set<BlockPos> seenMasters = new HashSet<>();
        int stones = 0;
        int stored = 0;
        int capacity = 0;
        for (Direction direction : Direction.values()) {
            BlockPos targetPos = worldPosition.relative(direction);
            WardStoneBlockEntity wardStone = WardStoneBlockEntity.getOrCreate(serverLevel, targetPos).orElse(null);
            if (wardStone == null || !seenMasters.add(wardStone.getBlockPos())) {
                continue;
            }
            stones += wardStone.multiblockSize();
            stored += wardStone.magicFlow();
            capacity += wardStone.magicFlowCapacity();
        }
        if (seenMasters.isEmpty()) {
            return "No linked Ward Stone core.";
        }
        return stones + " Ward Stones linked, MF " + stored + "/" + capacity + ".";
    }

    @Override
    public int receiveMagicEnergy(MagicEnergyType type, int amount, boolean simulate) {
        if (type != MagicEnergyType.ARCANE || amount < MAGIC_ENERGY_PER_MAGIC_FLOW) {
            return 0;
        }
        return receiveArcaneMagic(amount, simulate);
    }

    @Override
    public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ENERGY) {
            return energyCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCapability.invalidate();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
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

    private int receiveRf(int maxReceive, boolean simulate) {
        if (!(level instanceof ServerLevel serverLevel) || maxReceive < RF_PER_MAGIC_FLOW) {
            return 0;
        }
        int remainingMagicFlow = Math.min(maxReceive, RF_MAX_RECEIVE) / RF_PER_MAGIC_FLOW;
        int acceptedMagicFlow = 0;
        Set<BlockPos> seenMasters = new HashSet<>();
        for (Direction direction : Direction.values()) {
            if (remainingMagicFlow <= 0) {
                break;
            }
            BlockPos targetPos = worldPosition.relative(direction);
            WardStoneBlockEntity wardStone = WardStoneBlockEntity.getOrCreate(serverLevel, targetPos).orElse(null);
            if (wardStone == null || !seenMasters.add(wardStone.getBlockPos())) {
                continue;
            }
            int accepted = wardStone.receiveMagicFlow(remainingMagicFlow, simulate);
            acceptedMagicFlow += accepted;
            remainingMagicFlow -= accepted;
            if (accepted > 0 && !simulate) {
                MagicTransferParticles.emit(serverLevel, worldPosition, wardStone.getBlockPos(), MagicEnergyType.ARCANE, accepted);
            }
        }
        return acceptedMagicFlow * RF_PER_MAGIC_FLOW;
    }

    private int receiveArcaneMagic(int amount, boolean simulate) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        int remainingMagicFlow = amount / MAGIC_ENERGY_PER_MAGIC_FLOW;
        int acceptedMagicFlow = 0;
        Set<BlockPos> seenMasters = new HashSet<>();
        for (Direction direction : Direction.values()) {
            if (remainingMagicFlow <= 0) {
                break;
            }
            BlockPos targetPos = worldPosition.relative(direction);
            WardStoneBlockEntity wardStone = WardStoneBlockEntity.getOrCreate(serverLevel, targetPos).orElse(null);
            if (wardStone == null || !seenMasters.add(wardStone.getBlockPos())) {
                continue;
            }
            int accepted = wardStone.receiveMagicFlow(remainingMagicFlow, simulate);
            acceptedMagicFlow += accepted;
            remainingMagicFlow -= accepted;
        }
        if (acceptedMagicFlow > 0 && !simulate) {
            setChangedAndUpdate();
        }
        return acceptedMagicFlow * MAGIC_ENERGY_PER_MAGIC_FLOW;
    }

    private void setChangedAndUpdate() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private final class ConverterEnergyStorage implements IEnergyStorage {
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return receiveRf(maxReceive, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return RF_MAX_RECEIVE;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
