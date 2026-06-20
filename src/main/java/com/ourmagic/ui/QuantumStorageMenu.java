package com.ourmagic.ui;

import com.ourmagic.registry.ModBlocks;
import com.ourmagic.registry.ModMenus;
import com.ourmagic.storage.QuantumStorageNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class QuantumStorageMenu extends AbstractContainerMenu {
    private final Player player;
    private final BlockPos corePos;
    private final ContainerLevelAccess access;

    public QuantumStorageMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public QuantumStorageMenu(int containerId, Inventory inventory, BlockPos corePos) {
        super(ModMenus.QUANTUM_STORAGE.get(), containerId);
        this.player = inventory.player;
        this.corePos = corePos.immutable();
        this.access = ContainerLevelAccess.create(inventory.player.level(), corePos);
    }

    public BlockPos corePos() {
        return corePos;
    }

    public List<QuantumStorageNetwork.Entry> entries() {
        return player.level() instanceof ServerLevel serverLevel ? QuantumStorageNetwork.entries(serverLevel, corePos) : List.of();
    }

    public QuantumStorageNetwork.Summary summary() {
        return player.level() instanceof ServerLevel serverLevel ? QuantumStorageNetwork.summary(serverLevel, corePos) : new QuantumStorageNetwork.Summary(0, 0, 0, 0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.QUANTUM_STORAGE_CORE.get());
    }
}
