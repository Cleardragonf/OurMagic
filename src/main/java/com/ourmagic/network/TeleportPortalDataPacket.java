package com.ourmagic.network;

import com.ourmagic.client.TeleportPortalScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public record TeleportPortalDataPacket(BlockPos source, String name, Optional<BlockPos> target, List<Entry> entries) {
    public static void encode(TeleportPortalDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.source);
        buffer.writeUtf(packet.name, 64);
        buffer.writeBoolean(packet.target.isPresent());
        packet.target.ifPresent(buffer::writeBlockPos);
        buffer.writeCollection(packet.entries, (buf, entry) -> {
            buf.writeBlockPos(entry.pos());
            buf.writeUtf(entry.name(), 64);
            buf.writeUtf(entry.status(), 96);
        });
    }

    public static TeleportPortalDataPacket decode(FriendlyByteBuf buffer) {
        BlockPos source = buffer.readBlockPos();
        String name = buffer.readUtf(64);
        Optional<BlockPos> target = buffer.readBoolean() ? Optional.of(buffer.readBlockPos()) : Optional.empty();
        List<Entry> entries = buffer.readList(buf -> new Entry(buf.readBlockPos(), buf.readUtf(64), buf.readUtf(96)));
        return new TeleportPortalDataPacket(source, name, target, entries);
    }

    public static void handle(TeleportPortalDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TeleportPortalScreen.open(packet)));
        context.setPacketHandled(true);
    }

    public record Entry(BlockPos pos, String name, String status) {
    }
}
