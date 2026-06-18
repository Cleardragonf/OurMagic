package com.ourmagic.network;

import com.ourmagic.block.entity.TeleportPortalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public record TeleportPortalUpdatePacket(BlockPos source, String name, Optional<BlockPos> target) {
    public static void encode(TeleportPortalUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.source);
        buffer.writeUtf(packet.name, 64);
        buffer.writeBoolean(packet.target.isPresent());
        packet.target.ifPresent(buffer::writeBlockPos);
    }

    public static TeleportPortalUpdatePacket decode(FriendlyByteBuf buffer) {
        BlockPos source = buffer.readBlockPos();
        String name = buffer.readUtf(64);
        Optional<BlockPos> target = buffer.readBoolean() ? Optional.of(buffer.readBlockPos()) : Optional.empty();
        return new TeleportPortalUpdatePacket(source, name, target);
    }

    public static void handle(TeleportPortalUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.level() instanceof ServerLevel level)) {
                return;
            }
            if (player.distanceToSqr(packet.source.getX() + 0.5D, packet.source.getY() + 0.5D, packet.source.getZ() + 0.5D) > 64.0D) {
                return;
            }
            TeleportPortalBlockEntity.managedPortalAt(level, packet.source).ifPresent(portal -> {
                if (!portal.canManage(level, player)) {
                    player.displayClientMessage(Component.literal("You cannot manage this portal."), true);
                    return;
                }
                TeleportPortalBlockEntity.LinkResult result = portal.setManagedLink(level, player, packet.target, packet.name);
                player.displayClientMessage(Component.literal(result.message()), true);
                portal.openManagement(player);
            });
        });
        context.setPacketHandled(true);
    }
}
