package com.ourmagic.magic.spell;

import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MissileSpell extends BaseSpell {
    public MissileSpell() {
        super("missile", 8, 12, UPGRADE_MULTISTRIKE);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        int casts = multistrikeCasts(data);
        for (int i = 0; i < casts; i++) {
            ThrownTrident missile = new ThrownTrident(level, player, wand.copy());
            missile.setBaseDamage(8.0D * data.power() * data.activeDamageMultiplier() * multistrikePowerMultiplier(data));
            missile.shootFromRotation(player, player.getXRot(), player.getYRot() + (i - (casts - 1) / 2.0F) * 4.0F, 0, 1.6F * data.power(), 0.5F);
            level.addFreshEntity(missile);
        }
        Vec3 end = player.getEyePosition().add(player.getLookAngle().scale(10 * data.power()));
        beam(player, end, ParticleTypes.CRIT);
        beam(player, end, ParticleTypes.WITCH);
        return true;
    }
}
