package com.ourmagic.ui;

import com.ourmagic.block.entity.ArcaneQuarryBlockEntity;
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

public class ArcaneQuarryMenu extends AbstractContainerMenu {
    private final ArcaneQuarryBlockEntity quarry;
    private final ContainerLevelAccess access;

    public ArcaneQuarryMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public ArcaneQuarryMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ARCANE_QUARRY.get(), containerId);
        this.quarry = inventory.player.level().getBlockEntity(pos) instanceof ArcaneQuarryBlockEntity quarryBlock ? quarryBlock : null;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);
        if (quarry != null) {
            for (int i = 0; i < 7; i++) {
                addSlot(new SlotItemHandler(quarry.upgrades(), i, 34 + i * 24, 78));
            }
        }
        addPlayerInventory(inventory);
    }

    public ArcaneQuarryBlockEntity quarry() {
        return quarry;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < 7) {
            if (!moveItemStackTo(stack, 7, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 7, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ARCANE_QUARRY.get());
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 34 + col * 18, 110 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 34 + col * 18, 168));
        }
    }
}
