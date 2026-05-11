package com.ourmagic.network;

import com.ourmagic.magic.spell.runtime.Scrying;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ScryCommandPacket(Command command) {
    public static void encode(ScryCommandPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.command);
    }

    public static ScryCommandPacket decode(FriendlyByteBuf buffer) {
        return new ScryCommandPacket(buffer.readEnum(Command.class));
    }

    public static void handle(ScryCommandPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                Scrying.handleCommand(player, packet.command);
            }
        });
        context.setPacketHandled(true);
    }

    public enum Command {
        PREVIOUS,
        NEXT,
        RENEW,
        EXIT
    }
}
