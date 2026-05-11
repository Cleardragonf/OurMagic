package com.ourmagic.network;

import com.ourmagic.magic.spell.runtime.Scrying;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ScrySelectPacket(int index) {
    public static void encode(ScrySelectPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.index);
    }

    public static ScrySelectPacket decode(FriendlyByteBuf buffer) {
        return new ScrySelectPacket(buffer.readVarInt());
    }

    public static void handle(ScrySelectPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Scrying.viewSelected(player, packet.index);
            }
        });
        context.setPacketHandled(true);
    }
}
