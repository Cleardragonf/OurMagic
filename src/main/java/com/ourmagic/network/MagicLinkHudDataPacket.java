package com.ourmagic.network;

import com.ourmagic.client.MagicLinkHudData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record MagicLinkHudDataPacket(BlockPos pos, String title, List<String> lines) {
    public static void encode(MagicLinkHudDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeUtf(packet.title, 64);
        buffer.writeVarInt(packet.lines.size());
        for (String line : packet.lines) {
            buffer.writeUtf(line, 96);
        }
    }

    public static MagicLinkHudDataPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        String title = buffer.readUtf(64);
        int count = Math.min(8, buffer.readVarInt());
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            lines.add(buffer.readUtf(96));
        }
        return new MagicLinkHudDataPacket(pos, title, lines);
    }

    public static void handle(MagicLinkHudDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level != null) {
                MagicLinkHudData.set(packet.pos, packet.title, packet.lines, minecraft.level.getGameTime());
            }
        });
        context.setPacketHandled(true);
    }
}
