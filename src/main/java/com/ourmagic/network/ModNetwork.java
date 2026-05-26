package com.ourmagic.network;

import com.ourmagic.OurMagic;
import com.ourmagic.mana.PlayerMana;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
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
        CHANNEL.messageBuilder(FocusCastPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FocusCastPacket::encode)
                .decoder(FocusCastPacket::decode)
                .consumerMainThread(FocusCastPacket::handle)
                .add();
        CHANNEL.messageBuilder(ChantingPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ChantingPacket::encode)
                .decoder(ChantingPacket::decode)
                .consumerMainThread(ChantingPacket::handle)
                .add();
        CHANNEL.messageBuilder(WandSelectSpellPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(WandSelectSpellPacket::encode)
                .decoder(WandSelectSpellPacket::decode)
                .consumerMainThread(WandSelectSpellPacket::handle)
                .add();
        CHANNEL.messageBuilder(PlayerUpgradePacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PlayerUpgradePacket::encode)
                .decoder(PlayerUpgradePacket::decode)
                .consumerMainThread(PlayerUpgradePacket::handle)
                .add();
        CHANNEL.messageBuilder(ScryMarksPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ScryMarksPacket::encode)
                .decoder(ScryMarksPacket::decode)
                .consumerMainThread(ScryMarksPacket::handle)
                .add();
        CHANNEL.messageBuilder(ScrySelectPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ScrySelectPacket::encode)
                .decoder(ScrySelectPacket::decode)
                .consumerMainThread(ScrySelectPacket::handle)
                .add();
        CHANNEL.messageBuilder(ScryStatePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ScryStatePacket::encode)
                .decoder(ScryStatePacket::decode)
                .consumerMainThread(ScryStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(ScryCommandPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ScryCommandPacket::encode)
                .decoder(ScryCommandPacket::decode)
                .consumerMainThread(ScryCommandPacket::handle)
                .add();
        CHANNEL.messageBuilder(WardLightStatePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(WardLightStatePacket::encode)
                .decoder(WardLightStatePacket::decode)
                .consumerMainThread(WardLightStatePacket::handle)
                .add();
        CHANNEL.messageBuilder(IllusionDecoyPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(IllusionDecoyPacket::encode)
                .decoder(IllusionDecoyPacket::decode)
                .consumerMainThread(IllusionDecoyPacket::handle)
                .add();
        CHANNEL.messageBuilder(StunStatePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StunStatePacket::encode)
                .decoder(StunStatePacket::decode)
                .consumerMainThread(StunStatePacket::handle)
                .add();
    }

    public static void syncMana(ServerPlayer player, PlayerMana mana) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ManaSyncPacket(mana.mana(), mana.maxMana(), mana.regen(),
                mana.magicLevel(), mana.magicXp(), mana.magicXpToNextLevel(), mana.magicPoints()));
    }

    public static void sendScryMarks(ServerPlayer player, java.util.List<ScryMarksPacket.Entry> entries) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ScryMarksPacket(entries));
    }

    public static void syncScryState(ServerPlayer player, boolean active, String targetName, int targetIndex, int targetCount, int ticksRemaining) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ScryStatePacket(active, targetName, targetIndex, targetCount, ticksRemaining));
    }

    public static void syncWardLight(ServerPlayer player, boolean active) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new WardLightStatePacket(active));
    }

    public static void syncStunState(ServerPlayer player, boolean active) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StunStatePacket(active));
    }

    public static void sendIllusionDecoy(ServerLevel level, Vec3 position, IllusionDecoyPacket packet) {
        double maxDistanceSqr = 128.0D * 128.0D;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(position) <= maxDistanceSqr) {
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }
}
