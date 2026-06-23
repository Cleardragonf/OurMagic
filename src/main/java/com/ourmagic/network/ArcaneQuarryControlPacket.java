package com.ourmagic.network;

import com.ourmagic.block.entity.ArcaneQuarryBlockEntity;
import com.ourmagic.ui.ArcaneQuarryMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ArcaneQuarryControlPacket(Action action) {
    public static void encode(ArcaneQuarryControlPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.action);
    }

    public static ArcaneQuarryControlPacket decode(FriendlyByteBuf buffer) {
        return new ArcaneQuarryControlPacket(buffer.readEnum(Action.class));
    }

    public static void handle(ArcaneQuarryControlPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof ArcaneQuarryMenu menu)) {
                return;
            }
            ArcaneQuarryBlockEntity quarry = menu.quarry();
            if (quarry == null) {
                return;
            }
            switch (packet.action) {
                case TOGGLE_PAUSE -> quarry.togglePaused();
                case RESET_AREA -> quarry.resetArea();
                case BIND_MARKERS -> quarry.bindToNearestMarkers(player);
            }
        });
        context.setPacketHandled(true);
    }

    public enum Action {
        TOGGLE_PAUSE,
        RESET_AREA,
        BIND_MARKERS
    }
}
