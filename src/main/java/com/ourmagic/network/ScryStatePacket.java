package com.ourmagic.network;

import com.ourmagic.client.ClientScryData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ScryStatePacket(boolean active, String targetName, int targetIndex, int targetCount, int ticksRemaining) {
    public static void encode(ScryStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeUtf(packet.targetName);
        buffer.writeVarInt(packet.targetIndex);
        buffer.writeVarInt(packet.targetCount);
        buffer.writeVarInt(packet.ticksRemaining);
    }

    public static ScryStatePacket decode(FriendlyByteBuf buffer) {
        return new ScryStatePacket(buffer.readBoolean(), buffer.readUtf(128), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(ScryStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientScryData.set(packet.active, packet.targetName, packet.targetIndex, packet.targetCount, packet.ticksRemaining)));
        context.setPacketHandled(true);
    }
}
