package com.ourmagic.magic.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class MagicTransferParticles {
    private static final double VIEW_DISTANCE_SQR = 64.0D * 64.0D;

    private MagicTransferParticles() {
    }

    public static void emit(Level level, BlockPos from, BlockPos to, MagicEnergyType type, int amount) {
        if (!(level instanceof ServerLevel serverLevel) || amount <= 0) {
            return;
        }

        Vec3 start = Vec3.atCenterOf(from);
        Vec3 end = Vec3.atCenterOf(to);
        Vec3 delta = end.subtract(start);
        double length = delta.length();
        if (length <= 0.0D) {
            return;
        }

        int points = Math.max(3, Math.min(18, 2 + amount / 20));
        int thickness = Math.max(1, Math.min(5, 1 + amount / 40));
        DustParticleOptions particle = new DustParticleOptions(color(type), Math.min(2.5F, 0.75F + thickness * 0.22F));
        Vec3 midpoint = start.add(delta.scale(0.5D));

        for (ServerPlayer player : serverLevel.players()) {
            if (!ManaSight.canSeeManaAccumulation(player) || player.distanceToSqr(midpoint) > VIEW_DISTANCE_SQR) {
                continue;
            }

            for (int i = 0; i <= points; i++) {
                Vec3 point = start.add(delta.scale(i / (double) points));
                serverLevel.sendParticles(player, particle, true, point.x, point.y, point.z,
                        thickness, 0.025D * thickness, 0.025D * thickness, 0.025D * thickness, 0.0D);
            }
        }
    }

    private static Vector3f color(MagicEnergyType type) {
        int color = type.color();
        return new Vector3f(
                ((color >> 16) & 0xFF) / 255.0F,
                ((color >> 8) & 0xFF) / 255.0F,
                (color & 0xFF) / 255.0F);
    }
}
