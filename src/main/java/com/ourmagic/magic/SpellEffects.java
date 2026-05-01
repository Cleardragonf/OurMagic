package com.ourmagic.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SpellEffects {
    private SpellEffects() {
    }

    public static void strikeLightning(Level level, ServerPlayer player, Vec3 at) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return;
        }
        bolt.moveTo(at);
        bolt.setCause(player);
        serverLevel.addFreshEntity(bolt);
    }
}
