package com.ourmagic.magic.spell;

import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ArrowSpell extends BaseSpell {
    public ArrowSpell() {
        super("arrow", 10, 14, UPGRADE_MULTISTRIKE);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        int casts = multistrikeCasts(data);
        for (int i = 0; i < casts; i++) {
            Arrow arrow = new Arrow(level, player);
            arrow.setBaseDamage(3.0D * data.power() * data.activeDamageMultiplier() * multistrikePowerMultiplier(data));
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + (i - (casts - 1) / 2.0F) * 4.0F, 0.0F, 2.4F, 0.6F);
            level.addFreshEntity(arrow);
        }
        Vec3 end = player.getEyePosition().add(player.getLookAngle().scale(12 * data.power()));
        beam(player, end, ParticleTypes.ENCHANTED_HIT);
        return true;
    }
}
