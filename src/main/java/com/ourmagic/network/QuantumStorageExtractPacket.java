package com.ourmagic.network;

import com.ourmagic.storage.QuantumStorageNetwork;
import com.ourmagic.ui.QuantumStorageMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record QuantumStorageExtractPacket(int index, boolean fullStack) {
    public static void encode(QuantumStorageExtractPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.index);
        buffer.writeBoolean(packet.fullStack);
    }

    public static QuantumStorageExtractPacket decode(FriendlyByteBuf buffer) {
        return new QuantumStorageExtractPacket(buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(QuantumStorageExtractPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level() instanceof ServerLevel level) || !(player.containerMenu instanceof QuantumStorageMenu menu)) {
                return;
            }
            int amount = packet.fullStack ? 64 : 1;
            ItemStack extracted = QuantumStorageNetwork.extract(level, menu.corePos(), packet.index, amount);
            if (!extracted.isEmpty() && !player.getInventory().add(extracted)) {
                player.drop(extracted, false);
            }
            QuantumStorageDataPacket.send(player, level, menu.corePos());
        });
        context.setPacketHandled(true);
    }
}
