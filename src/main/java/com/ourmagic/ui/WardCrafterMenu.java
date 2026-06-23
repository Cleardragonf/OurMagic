package com.ourmagic.ui;

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

public class WardCrafterMenu extends AbstractContainerMenu {
    private static final int INVENTORY_X = 99;
    private static final int INVENTORY_Y = 326;
    private static final int HOTBAR_Y = 384;

    private final BlockPos pos;
    private final ContainerLevelAccess access;

    public WardCrafterMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public WardCrafterMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.WARD_CRAFTER.get(), containerId);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        addPlayerInventory(inventory);
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot source = slots.get(index);
        if (!source.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = source.getItem();
        ItemStack copy = original.copy();
        if (index < 27) {
            if (!moveItemStackTo(original, 27, 36, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(original, 0, 27, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) {
            source.set(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.WARD_CRAFTER.get());
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, INVENTORY_Y + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HOTBAR_Y));
        }
    }
}
