package com.ourmagic.network;

import com.ourmagic.storage.QuantumStorageNetwork;
import com.ourmagic.ui.QuantumStorageMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record QuantumStorageInsertPacket(Mode mode, int index) {
    public static void encode(QuantumStorageInsertPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.mode);
        buffer.writeVarInt(packet.index);
    }

    public static QuantumStorageInsertPacket decode(FriendlyByteBuf buffer) {
        return new QuantumStorageInsertPacket(buffer.readEnum(Mode.class), buffer.readVarInt());
    }

    public static void handle(QuantumStorageInsertPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level() instanceof ServerLevel level) || !(player.containerMenu instanceof QuantumStorageMenu menu)) {
                return;
            }
            switch (packet.mode) {
                case CARRIED -> insertCarried(player, level, menu);
                case MATCHING -> insertMatching(player, level, menu, packet.index);
                case ALL -> insertAll(player, level, menu);
            }
            QuantumStorageDataPacket.send(player, level, menu.corePos());
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
        });
        context.setPacketHandled(true);
    }

    private static void insertCarried(ServerPlayer player, ServerLevel level, QuantumStorageMenu menu) {
        AbstractContainerMenu container = player.containerMenu;
        ItemStack carried = container.getCarried();
        if (carried.isEmpty()) {
            return;
        }
        ItemStack remaining = QuantumStorageNetwork.insert(level, menu.corePos(), carried);
        container.setCarried(remaining);
    }

    private static void insertMatching(ServerPlayer player, ServerLevel level, QuantumStorageMenu menu, int index) {
        List<QuantumStorageNetwork.Entry> entries = QuantumStorageNetwork.entries(level, menu.corePos());
        if (index < 0 || index >= entries.size()) {
            return;
        }
        ItemStack target = entries.get(index).stack();
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, target)) {
                ItemStack remaining = QuantumStorageNetwork.insert(level, menu.corePos(), stack);
                player.getInventory().items.set(slot, remaining);
            }
        }
    }

    private static void insertAll(ServerPlayer player, ServerLevel level, QuantumStorageMenu menu) {
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            ItemStack stack = player.getInventory().items.get(slot);
            if (!stack.isEmpty()) {
                ItemStack remaining = QuantumStorageNetwork.insert(level, menu.corePos(), stack);
                player.getInventory().items.set(slot, remaining);
            }
        }
    }

    public enum Mode {
        CARRIED,
        MATCHING,
        ALL
    }
}
