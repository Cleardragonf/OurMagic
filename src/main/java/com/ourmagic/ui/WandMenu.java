package com.ourmagic.ui;

import com.ourmagic.registry.ModItems;
import com.ourmagic.registry.ModMenus;
import com.ourmagic.wand.WandData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class WandMenu extends AbstractContainerMenu {
    private final Player player;
    private final InteractionHand hand;

    public WandMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readEnum(InteractionHand.class));
    }

    public WandMenu(int containerId, Inventory inventory, InteractionHand hand) {
        super(ModMenus.WAND.get(), containerId);
        this.player = inventory.player;
        this.hand = hand;
    }

    public ItemStack wand() {
        return player.getItemInHand(hand);
    }

    public InteractionHand hand() {
        return hand;
    }

    public WandData wandData() {
        return WandData.read(wand());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack stack = wand();
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
    }
}
