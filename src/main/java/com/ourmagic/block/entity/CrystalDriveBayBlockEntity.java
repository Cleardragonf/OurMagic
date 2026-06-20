package com.ourmagic.block.entity;

import com.ourmagic.item.CrystalDriveItem;
import com.ourmagic.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public class CrystalDriveBayBlockEntity extends BlockEntity {
    private static final String TAG_DRIVES = "Drives";
    private final ItemStackHandler drives = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof CrystalDriveItem;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChangedAndUpdate();
        }
    };
    private final IItemHandler externalHandler = new DriveStorageHandler();
    private final LazyOptional<IItemHandler> driveCapability = LazyOptional.of(() -> drives);
    private final LazyOptional<IItemHandler> externalCapability = LazyOptional.of(() -> externalHandler);

    public CrystalDriveBayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_DRIVE_BAY.get(), pos, state);
    }

    public String statusLine() {
        int used = 0;
        int capacity = 0;
        int installed = 0;
        for (int i = 0; i < drives.getSlots(); i++) {
            ItemStack drive = drives.getStackInSlot(i);
            if (drive.getItem() instanceof CrystalDriveItem crystalDrive) {
                installed++;
                used += CrystalDriveItem.storedCount(drive);
                capacity += crystalDrive.capacity();
            }
        }
        return installed + "/" + drives.getSlots() + " drives, " + used + "/" + capacity + " items.";
    }

    public IItemHandler driveSlots() {
        return drives;
    }

    public boolean addDrive(ItemStack stack) {
        if (!(stack.getItem() instanceof CrystalDriveItem)) {
            return false;
        }
        for (int i = 0; i < drives.getSlots(); i++) {
            if (drives.getStackInSlot(i).isEmpty()) {
                ItemStack drive = stack.copy();
                drive.setCount(1);
                drives.setStackInSlot(i, drive);
                stack.shrink(1);
                setChangedAndUpdate();
                return true;
            }
        }
        return false;
    }

    public ItemStack removeDrive() {
        for (int i = drives.getSlots() - 1; i >= 0; i--) {
            ItemStack drive = drives.getStackInSlot(i);
            if (!drive.isEmpty()) {
                drives.setStackInSlot(i, ItemStack.EMPTY);
                setChangedAndUpdate();
                return drive;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        drives.deserializeNBT(tag.getCompound(TAG_DRIVES));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(TAG_DRIVES, drives.serializeNBT());
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

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return side == null ? driveCapability.cast() : externalCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        driveCapability.invalidate();
        externalCapability.invalidate();
    }

    private void setChangedAndUpdate() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private class DriveStorageHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return drives.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemStack drive = drives.getStackInSlot(slot);
            return drive.getItem() instanceof CrystalDriveItem ? CrystalDriveItem.storedItem(drive) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack remaining = stack.copy();
            for (int i = 0; i < drives.getSlots() && !remaining.isEmpty(); i++) {
                ItemStack drive = drives.getStackInSlot(i);
                if (drive.getItem() instanceof CrystalDriveItem crystalDrive) {
                    int accepted = crystalDrive.insert(drive, remaining, simulate);
                    if (accepted > 0) {
                        remaining.shrink(accepted);
                        if (!simulate) {
                            setChangedAndUpdate();
                        }
                    }
                }
            }
            return remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= drives.getSlots()) {
                return ItemStack.EMPTY;
            }
            ItemStack drive = drives.getStackInSlot(slot);
            if (!(drive.getItem() instanceof CrystalDriveItem crystalDrive)) {
                return ItemStack.EMPTY;
            }
            ItemStack extracted = crystalDrive.extract(drive, amount, simulate);
            if (!simulate && !extracted.isEmpty()) {
                setChangedAndUpdate();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    }
}
