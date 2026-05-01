package com.ourmagic.magic.spell;

import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BlinkSpell extends BaseSpell {
    public BlinkSpell() {
        super("blink", 28, 70, UPGRADE_RANGE);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        HitResult hit = raycast(player, 28 * data.power() * rangeMultiplier(data));
        Vec3 target = hit.getType() == HitResult.Type.MISS
                ? player.position().add(player.getLookAngle().scale(16 * data.power() * rangeMultiplier(data)))
                : hit.getLocation().subtract(player.getLookAngle().normalize().scale(1.2D));
        Vec3 before = player.position().add(0, 1.0D, 0);
        beam(player, target.add(0, 1.0D, 0), ParticleTypes.PORTAL);
        burst(level, before, ParticleTypes.REVERSE_PORTAL, 35, 0.55D, 0.08D);
        player.teleportTo(target.x, target.y, target.z);
        player.resetFallDistance();
        burst(level, target.add(0, 1.0D, 0), ParticleTypes.PORTAL, 45, 0.65D, 0.08D);
        return true;
    }
}
