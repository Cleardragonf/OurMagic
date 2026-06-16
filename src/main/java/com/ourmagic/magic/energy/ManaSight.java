package com.ourmagic.magic.energy;

import net.minecraft.server.level.ServerPlayer;

public final class ManaSight {
    private ManaSight() {
    }

    public static boolean canSeeManaAccumulation(ServerPlayer player) {
        return player.getAbilities().instabuild
                || player.isSpectator()
                || player.getServer() != null && player.getServer().getPlayerList().isOp(player.getGameProfile());
    }
}
