package com.ourmagic.network;

import com.ourmagic.registry.ModItems;
import com.ourmagic.ui.WandMenu;
import com.ourmagic.wand.WandData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record WandSelectSpellPacket(InteractionHand hand, Mode mode, int slot, int spellIndex) {
    public static void encode(WandSelectSpellPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeEnum(packet.mode);
        buffer.writeVarInt(packet.slot);
        buffer.writeVarInt(packet.spellIndex);
    }

    public static WandSelectSpellPacket decode(FriendlyByteBuf buffer) {
        return new WandSelectSpellPacket(buffer.readEnum(InteractionHand.class), buffer.readEnum(Mode.class), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void handle(WandSelectSpellPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ItemStack wand = player.getItemInHand(packet.hand);
            if (!wand.is(ModItems.WAND.get()) && !wand.is(ModItems.ADMIN_WAND.get())) {
                return;
            }

            WandData data = WandData.read(wand);
            boolean changed = switch (packet.mode) {
                case SELECT_INDEX -> data.setActiveSpellIndex(packet.spellIndex);
                case SELECT_ASSIGNED -> data.selectAssignedSpell(packet.slot);
                case ASSIGN -> data.assignSpellSlot(packet.slot, packet.spellIndex);
            };
            if (!changed) {
                return;
            }

            data.save(wand);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            player.inventoryMenu.broadcastChanges();
            syncHandStack(player, packet.hand, wand);
        });
        context.setPacketHandled(true);
    }

    private static void syncHandStack(ServerPlayer player, InteractionHand hand, ItemStack wand) {
        if (player.containerMenu instanceof WandMenu menu && menu.hand() == hand) {
            menu.broadcastChanges();
        }

        if (hand == InteractionHand.OFF_HAND) {
            player.connection.send(new ClientboundContainerSetSlotPacket(ClientboundContainerSetSlotPacket.PLAYER_INVENTORY, 0, 45, wand.copy()));
            return;
        }

        int slot = player.getInventory().selected;
        player.connection.send(new ClientboundContainerSetSlotPacket(ClientboundContainerSetSlotPacket.PLAYER_INVENTORY, 0, 36 + slot, wand.copy()));
    }

    public enum Mode {
        SELECT_INDEX,
        SELECT_ASSIGNED,
        ASSIGN
    }
}
