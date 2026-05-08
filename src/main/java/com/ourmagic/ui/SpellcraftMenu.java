package com.ourmagic.ui;

import com.ourmagic.registry.ModItems;
import com.ourmagic.registry.ModMenus;
import com.ourmagic.wand.WandData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SpellcraftMenu extends AbstractContainerMenu {
    private static final int INVENTORY_X = 124;
    private static final int INVENTORY_Y = 310;
    private static final int HOTBAR_Y = 364;

    private final Player player;
    private final InteractionHand hand;

    public SpellcraftMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readEnum(InteractionHand.class));
    }

    public SpellcraftMenu(int containerId, Inventory inventory, InteractionHand hand) {
        super(ModMenus.SPELLCRAFT.get(), containerId);
        this.player = inventory.player;
        this.hand = hand;
        addPlayerInventory(inventory);
    }

    public InteractionHand hand() {
        return hand;
    }

    public ItemStack wand() {
        return player.getItemInHand(hand);
    }

    public WandData wandData() {
        return WandData.read(wand());
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
        ItemStack stack = wand();
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
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
