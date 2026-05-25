package com.ourmagic.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerTitles {
    private PlayerTitles() {
    }

    public static void show(ServerPlayer player, Component title) {
        show(player, title, Component.empty(), 5, 45, 10);
    }

    public static void show(ServerPlayer player, Component title, Component subtitle) {
        show(player, title, subtitle, 5, 45, 10);
    }

    public static void show(ServerPlayer player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }
}
