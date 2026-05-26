package com.ourmagic.network;

import com.ourmagic.ui.WandMenu;
import com.ourmagic.wand.WandData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpgradeSpellPacket(int spellIndex, String upgrade) {
    public static void encode(UpgradeSpellPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.spellIndex);
        buffer.writeUtf(packet.upgrade);
    }

    public static UpgradeSpellPacket decode(FriendlyByteBuf buffer) {
        return new UpgradeSpellPacket(buffer.readVarInt(), buffer.readUtf(32));
    }

    public static void handle(UpgradeSpellPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof WandMenu menu)) {
                return;
            }

            ItemStack wand = menu.wand();
            WandData data = WandData.read(wand);
            if (data.upgradeSpell(packet.spellIndex, packet.upgrade)) {
                data.save(wand);
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
                player.inventoryMenu.broadcastChanges();
                syncHandStack(player, menu, wand);
            }
        });
        context.setPacketHandled(true);
    }

    private static void syncHandStack(ServerPlayer player, WandMenu menu, ItemStack wand) {
        if (menu.hand() == InteractionHand.OFF_HAND) {
            player.connection.send(new ClientboundContainerSetSlotPacket(ClientboundContainerSetSlotPacket.PLAYER_INVENTORY, 0, net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND, wand.copy()));
            return;
        }

        int slot = player.getInventory().selected;
        player.connection.send(new ClientboundContainerSetSlotPacket(ClientboundContainerSetSlotPacket.PLAYER_INVENTORY, 0, slot, wand.copy()));
    }
}
