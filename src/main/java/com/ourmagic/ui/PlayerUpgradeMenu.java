package com.ourmagic.ui;

import com.ourmagic.registry.ModItems;
import com.ourmagic.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class PlayerUpgradeMenu extends AbstractContainerMenu {
    private final Player player;

    public PlayerUpgradeMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory);
    }

    public PlayerUpgradeMenu(int containerId, Inventory inventory) {
        super(ModMenus.PLAYER_UPGRADES.get(), containerId);
        this.player = inventory.player;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack offhand = this.player.getOffhandItem();
        return offhand.is(ModItems.WAND.get()) || offhand.is(ModItems.ADMIN_WAND.get());
    }
}
