package com.ourmagic.network;

import com.ourmagic.ui.QuantumStorageMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record QuantumStorageQueryPacket(BlockPos corePos) {
    public static void encode(QuantumStorageQueryPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.corePos);
    }

    public static QuantumStorageQueryPacket decode(FriendlyByteBuf buffer) {
        return new QuantumStorageQueryPacket(buffer.readBlockPos());
    }

    public static void handle(QuantumStorageQueryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level() instanceof ServerLevel level) || !(player.containerMenu instanceof QuantumStorageMenu menu)) {
                return;
            }
            if (!menu.corePos().equals(packet.corePos) || player.distanceToSqr(packet.corePos.getCenter()) > 64.0D) {
                return;
            }
            QuantumStorageDataPacket.send(player, level, packet.corePos);
        });
        context.setPacketHandled(true);
    }
}
