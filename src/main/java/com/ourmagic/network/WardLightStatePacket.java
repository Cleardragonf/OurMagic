package com.ourmagic.network;

import com.ourmagic.client.ClientWardLightData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record WardLightStatePacket(boolean active) {
    public static void encode(WardLightStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
    }

    public static WardLightStatePacket decode(FriendlyByteBuf buffer) {
        return new WardLightStatePacket(buffer.readBoolean());
    }

    public static void handle(WardLightStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientWardLightData.setActive(packet.active)));
        context.setPacketHandled(true);
    }
}
