package com.ourmagic.network;

import com.ourmagic.item.WandItem;
import com.ourmagic.registry.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record FocusCastPacket(InteractionHand hand, float multiplier) {
    public static void encode(FocusCastPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeFloat(packet.multiplier);
    }

    public static FocusCastPacket decode(FriendlyByteBuf buffer) {
        return new FocusCastPacket(buffer.readEnum(InteractionHand.class), buffer.readFloat());
    }

    public static void handle(FocusCastPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ItemStack stack = player.getItemInHand(packet.hand);
            if (!stack.is(ModItems.WAND.get()) && !stack.is(ModItems.ADMIN_WAND.get())) {
                return;
            }

            WandItem.castActiveSpell(player, stack, Math.max(1.0F, Math.min(100.0F, packet.multiplier)));
        });
        context.setPacketHandled(true);
    }
}
