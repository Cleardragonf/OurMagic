package com.ourmagic.network;

import com.ourmagic.client.ClientManaData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ManaSyncPacket(int mana, int maxMana, int regen) {
    public static void encode(ManaSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.mana);
        buffer.writeVarInt(packet.maxMana);
        buffer.writeVarInt(packet.regen);
    }

    public static ManaSyncPacket decode(FriendlyByteBuf buffer) {
        return new ManaSyncPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(ManaSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientManaData.set(packet.mana, packet.maxMana, packet.regen)));
        context.setPacketHandled(true);
    }
}
