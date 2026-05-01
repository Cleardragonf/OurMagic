package com.ourmagic.magic.spell;

import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BlastSpell extends BaseSpell {
    public BlastSpell() {
        super("blast", 24, 50, UPGRADE_MULTISTRIKE);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        HitResult hit = raycast(player, 24 * data.power());
        Vec3 at = hit.getType() == HitResult.Type.MISS ? player.getEyePosition().add(player.getLookAngle().scale(16)) : hit.getLocation();
        sparkleBeam(player, at);
        for (int i = 0; i < multistrikeCasts(data); i++) {
            Vec3 strikeAt = multistrikeTarget(player, at, i);
            burst(level, strikeAt, ParticleTypes.EXPLOSION, 3, 0.25D, 0.0D);
            burst(level, strikeAt, ParticleTypes.FLASH, 1, 0.0D, 0.0D);
            level.explode(player, strikeAt.x, strikeAt.y, strikeAt.z, 1.5F * data.power() * data.activeDamageMultiplier() * multistrikePowerMultiplier(data), ExplosionInteraction.NONE);
        }
        return true;
    }
}
