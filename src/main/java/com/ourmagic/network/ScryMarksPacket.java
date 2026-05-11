package com.ourmagic.network;

import com.ourmagic.client.ScrySelectionScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record ScryMarksPacket(List<Entry> entries) {
    public static void encode(ScryMarksPacket packet, FriendlyByteBuf buffer) {
        buffer.writeCollection(packet.entries, (buf, entry) -> {
            buf.writeVarInt(entry.index());
            buf.writeUtf(entry.type());
            buf.writeUtf(entry.name());
        });
    }

    public static ScryMarksPacket decode(FriendlyByteBuf buffer) {
        return new ScryMarksPacket(buffer.readList(buf -> new Entry(buf.readVarInt(), buf.readUtf(32), buf.readUtf(128))));
    }

    public static void handle(ScryMarksPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ScrySelectionScreen.open(packet.entries)));
        context.setPacketHandled(true);
    }

    public record Entry(int index, String type, String name) {
    }
}
