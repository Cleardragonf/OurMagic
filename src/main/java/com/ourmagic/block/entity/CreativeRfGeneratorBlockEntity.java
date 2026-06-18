package com.ourmagic.block.entity;

import com.ourmagic.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class CreativeRfGeneratorBlockEntity extends BlockEntity {
    private static final int RF_PUSH_PER_SIDE_PER_TICK = 100_000;
    private static final int RF_EXTRACT_CAP = Integer.MAX_VALUE;

    private final CreativeEnergyStorage energyStorage = new CreativeEnergyStorage();
    private final LazyOptional<IEnergyStorage> energyCapability = LazyOptional.of(() -> energyStorage);
    private int lastTickPushed;

    public CreativeRfGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_RF_GENERATOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CreativeRfGeneratorBlockEntity generator) {
        int pushed = 0;
        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor == null) {
                continue;
            }
            pushed += neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite())
                    .map(storage -> storage.receiveEnergy(RF_PUSH_PER_SIDE_PER_TICK, false))
                    .orElse(0);
        }
        generator.lastTickPushed = pushed;
        generator.setChanged();
    }

    public String statusLine() {
        return "Unlimited RF. Pushes up to " + RF_PUSH_PER_SIDE_PER_TICK + " RF/t per side. Last tick: " + lastTickPushed + " RF.";
    }

    public int lastTickPushed() {
        return lastTickPushed;
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

    private static final class CreativeEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return Math.max(0, maxExtract);
        }

        @Override
        public int getEnergyStored() {
            return RF_EXTRACT_CAP;
        }

        @Override
        public int getMaxEnergyStored() {
            return RF_EXTRACT_CAP;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
