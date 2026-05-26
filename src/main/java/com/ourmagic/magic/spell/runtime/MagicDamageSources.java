package com.ourmagic.magic.spell.runtime;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class MagicDamageSources {
    private MagicDamageSources() {
    }

    public static DamageSource playerMagic(Level level, ServerPlayer player) {
        return level.damageSources().indirectMagic(player, player);
    }

    public static DamageSource playerMagicOrGeneric(ServerLevel level, UUID playerId) {
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        return player == null ? level.damageSources().magic() : playerMagic(level, player);
    }

    public static DamageSource reflectedMagic(Entity attacker) {
        if (attacker instanceof ServerPlayer player) {
            return playerMagic(player.level(), player);
        }
        return attacker.level().damageSources().magic();
    }
}
