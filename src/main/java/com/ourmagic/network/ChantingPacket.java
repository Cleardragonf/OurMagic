package com.ourmagic.network;

import com.ourmagic.magic.PlayerChanting;
import com.ourmagic.registry.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ChantingPacket(Mode mode, InteractionHand hand, int level) {
    public static void encode(ChantingPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.mode);
        buffer.writeEnum(packet.hand);
        buffer.writeVarInt(packet.level);
    }

    public static ChantingPacket decode(FriendlyByteBuf buffer) {
        return new ChantingPacket(buffer.readEnum(Mode.class), buffer.readEnum(InteractionHand.class), buffer.readVarInt());
    }

    public static void handle(ChantingPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = player.getItemInHand(packet.hand);
            if (!stack.is(ModItems.WAND.get()) && !stack.is(ModItems.ADMIN_WAND.get())) {
                PlayerChanting.cancel(player);
                return;
            }

            switch (packet.mode) {
                case START -> PlayerChanting.start(player, packet.hand);
                case UPDATE -> PlayerChanting.update(player, packet.hand, packet.level);
                case CANCEL -> PlayerChanting.cancel(player);
            }
        });
        context.setPacketHandled(true);
    }

    public enum Mode {
        START,
        UPDATE,
        CANCEL
    }
}
