package com.ourmagic.ui;

import com.ourmagic.block.entity.CrystalDriveBayBlockEntity;
import com.ourmagic.item.CrystalDriveItem;
import com.ourmagic.registry.ModBlocks;
import com.ourmagic.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class CrystalDriveBayMenu extends AbstractContainerMenu {
    private final CrystalDriveBayBlockEntity bay;
    private final ContainerLevelAccess access;

    public CrystalDriveBayMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public CrystalDriveBayMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.CRYSTAL_DRIVE_BAY.get(), containerId);
        this.bay = inventory.player.level().getBlockEntity(pos) instanceof CrystalDriveBayBlockEntity driveBay ? driveBay : null;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);

        if (bay != null) {
            for (int i = 0; i < 4; i++) {
                addSlot(new SlotItemHandler(bay.driveSlots(), i, 53 + i * 24, 35));
            }
        }
        addPlayerInventory(inventory);
    }

    public CrystalDriveBayBlockEntity bay() {
        return bay;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();
        if (index < 4) {
            if (!moveItemStackTo(stack, 4, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof CrystalDriveItem) {
            if (!moveItemStackTo(stack, 0, 4, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.CRYSTAL_DRIVE_BAY.get());
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }
}
