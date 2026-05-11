package com.ourmagic.network;

import com.ourmagic.mana.PlayerMana;
import com.ourmagic.ui.PlayerUpgradeMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PlayerUpgradePacket(String upgrade) {
    public static final String MAX_MANA = "max_mana";
    public static final String MANA_REGEN = "mana_regen";

    private static final int MAX_MANA_AMOUNT = 10;
    private static final int MANA_REGEN_AMOUNT = 1;

    public static void encode(PlayerUpgradePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.upgrade);
    }

    public static PlayerUpgradePacket decode(FriendlyByteBuf buffer) {
        return new PlayerUpgradePacket(buffer.readUtf(32));
    }

    public static void handle(PlayerUpgradePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof PlayerUpgradeMenu)) {
                return;
            }

            PlayerMana mana = PlayerMana.get(player);
            int cost = cost(packet.upgrade);
            if (cost <= 0 || !player.getAbilities().instabuild && mana.magicPoints() < cost) {
                return;
            }

            if (!player.getAbilities().instabuild && !mana.spendMagicPoints(cost)) {
                return;
            }

            switch (packet.upgrade) {
                case MAX_MANA -> mana.addMaxMana(MAX_MANA_AMOUNT);
                case MANA_REGEN -> mana.addRegen(MANA_REGEN_AMOUNT);
                default -> {
                    return;
                }
            }
            ModNetwork.syncMana(player, mana);
        });
        context.setPacketHandled(true);
    }

    public static int cost(String upgrade) {
        return switch (upgrade) {
            case MAX_MANA -> 1;
            case MANA_REGEN -> 2;
            default -> 0;
        };
    }
}
