package com.ourmagic.network;

import com.ourmagic.client.ClientQuantumStorageData;
import com.ourmagic.storage.QuantumStorageNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record QuantumStorageDataPacket(BlockPos corePos, ClientQuantumStorageData.Summary summary, List<ClientQuantumStorageData.Entry> entries) {
    public static void send(ServerPlayer player, ServerLevel level, BlockPos corePos) {
        QuantumStorageNetwork.Summary summary = QuantumStorageNetwork.summary(level, corePos);
        List<QuantumStorageNetwork.Entry> networkEntries = QuantumStorageNetwork.entries(level, corePos);
        List<ClientQuantumStorageData.Entry> entries = new ArrayList<>();
        for (int i = 0; i < networkEntries.size(); i++) {
            QuantumStorageNetwork.Entry entry = networkEntries.get(i);
            entries.add(new ClientQuantumStorageData.Entry(i, entry.stack(), entry.count(), entry.capacity()));
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new QuantumStorageDataPacket(corePos,
                new ClientQuantumStorageData.Summary(summary.bays(), summary.drives(), summary.used(), summary.capacity()),
                entries));
    }

    public static void encode(QuantumStorageDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.corePos);
        buffer.writeVarInt(packet.summary.bays());
        buffer.writeVarInt(packet.summary.drives());
        buffer.writeVarInt(packet.summary.used());
        buffer.writeVarInt(packet.summary.capacity());
        buffer.writeCollection(packet.entries, (buf, entry) -> {
            buf.writeVarInt(entry.index());
            buf.writeItem(entry.stack());
            buf.writeVarInt(entry.count());
            buf.writeVarInt(entry.capacity());
        });
    }

    public static QuantumStorageDataPacket decode(FriendlyByteBuf buffer) {
        BlockPos corePos = buffer.readBlockPos();
        ClientQuantumStorageData.Summary summary = new ClientQuantumStorageData.Summary(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        List<ClientQuantumStorageData.Entry> entries = buffer.readList(buf -> new ClientQuantumStorageData.Entry(buf.readVarInt(), buf.readItem(), buf.readVarInt(), buf.readVarInt()));
        return new QuantumStorageDataPacket(corePos, summary, entries);
    }

    public static void handle(QuantumStorageDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientQuantumStorageData.set(packet.corePos, packet.summary, packet.entries)));
        context.setPacketHandled(true);
    }
}
