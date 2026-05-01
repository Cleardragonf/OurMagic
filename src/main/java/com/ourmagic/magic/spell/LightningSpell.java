package com.ourmagic.magic.spell;

import com.ourmagic.magic.SpellEffects;
import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LightningSpell extends BaseSpell {
    public LightningSpell() {
        super("lightning", 35, 90, UPGRADE_CHAINING, UPGRADE_MULTISTRIKE);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        double range = 30 * data.power();
        Vec3 at = raycastEntity(player, range, entity -> entity instanceof LivingEntity)
                .map(hit -> hit.getEntity().position().add(0, hit.getEntity().getBbHeight() * 0.5D, 0))
                .orElseGet(() -> {
                    HitResult hit = raycast(player, range);
                    return hit.getType() == HitResult.Type.MISS ? player.getEyePosition().add(player.getLookAngle().scale(20)) : hit.getLocation();
                });
        beam(player, at, ParticleTypes.ELECTRIC_SPARK);
        burst(level, at, ParticleTypes.ELECTRIC_SPARK, 60, 0.9D, 0.12D);
        burst(level, at, ParticleTypes.FLASH, 1, 0.0D, 0.0D);
        int strikes = multistrikeCasts(data);
        for (int i = 0; i < strikes; i++) {
            Vec3 strikeAt = multistrikeTarget(player, at, i);
            SpellEffects.strikeLightning(level, player, strikeAt);
        }
        chainLivingTargetsFrom(level, player, at, data, ParticleTypes.ELECTRIC_SPARK, (target, targetPos) -> SpellEffects.strikeLightning(level, player, targetPos));
        return true;
    }
}
