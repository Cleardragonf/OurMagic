package com.ourmagic.magic.spell;

import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FireballSpell extends BaseSpell {
    public FireballSpell() {
        super("fireball", 18, 30, UPGRADE_MULTISTRIKE);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        for (int i = 0; i < multistrikeCasts(data); i++) {
            Vec3 direction = player.getLookAngle().add((player.getRandom().nextDouble() - 0.5D) * 0.04D * i, (player.getRandom().nextDouble() - 0.5D) * 0.02D * i, (player.getRandom().nextDouble() - 0.5D) * 0.04D * i);
            SmallFireball fireball = new SmallFireball(level, player, direction.x, direction.y, direction.z);
            fireball.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
            fireball.setDeltaMovement(fireball.getDeltaMovement().scale(data.activeDamageMultiplier() * multistrikePowerMultiplier(data)));
            level.addFreshEntity(fireball);
        }
        Vec3 end = player.getEyePosition().add(player.getLookAngle().scale(8));
        beam(player, end, ParticleTypes.FLAME);
        burst(level, player.getEyePosition().add(player.getLookAngle().scale(1.0D)), ParticleTypes.LAVA, 8, 0.12D, 0.04D);
        return true;
    }
}
