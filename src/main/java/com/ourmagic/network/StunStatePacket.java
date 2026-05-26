package com.ourmagic.network;

import com.ourmagic.client.ClientStunData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record StunStatePacket(boolean active) {
    public static void encode(StunStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
    }

    public static StunStatePacket decode(FriendlyByteBuf buffer) {
        return new StunStatePacket(buffer.readBoolean());
    }

    public static void handle(StunStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientStunData.setActive(packet.active)));
        context.setPacketHandled(true);
    }
}
