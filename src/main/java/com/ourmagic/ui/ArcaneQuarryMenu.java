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
import net.minecraft.world.item.ItemStack;

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
    }

    public ArcaneQuarryBlockEntity quarry() {
        return quarry;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.ARCANE_QUARRY.get());
    }
}
