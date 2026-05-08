package com.ourmagic.network;

import com.ourmagic.OurMagic;
import com.ourmagic.mana.PlayerMana;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static int packetId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(OurMagic.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private ModNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(ManaSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ManaSyncPacket::encode)
                .decoder(ManaSyncPacket::decode)
                .consumerMainThread(ManaSyncPacket::handle)
                .add();
        CHANNEL.messageBuilder(UpgradeSpellPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpgradeSpellPacket::encode)
                .decoder(UpgradeSpellPacket::decode)
                .consumerMainThread(UpgradeSpellPacket::handle)
                .add();
        CHANNEL.messageBuilder(CraftSpellPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CraftSpellPacket::encode)
                .decoder(CraftSpellPacket::decode)
                .consumerMainThread(CraftSpellPacket::handle)
                .add();
        CHANNEL.messageBuilder(ChantCastPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ChantCastPacket::encode)
                .decoder(ChantCastPacket::decode)
                .consumerMainThread(ChantCastPacket::handle)
                .add();
        CHANNEL.messageBuilder(WandSelectSpellPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(WandSelectSpellPacket::encode)
                .decoder(WandSelectSpellPacket::decode)
                .consumerMainThread(WandSelectSpellPacket::handle)
                .add();
    }

    public static void syncMana(ServerPlayer player, PlayerMana mana) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ManaSyncPacket(mana.mana(), mana.maxMana(), mana.regen()));
    }
}
