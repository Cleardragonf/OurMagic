package com.ourmagic.magic.spell;

import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class LevitateSpell extends BaseSpell {
    public LevitateSpell() {
        super("levitate", 22, 45, UPGRADE_DURATION);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        return raycastEntity(player, 20 * data.power(), entity -> entity instanceof LivingEntity)
                .map(hit -> {
                    beam(player, hit.getLocation(), ParticleTypes.END_ROD);
                    ring(level, hit.getEntity().position().add(0, 0.2D, 0), ParticleTypes.CLOUD, 0.8D, 20);
                    ((LivingEntity) hit.getEntity()).addEffect(new MobEffectInstance(MobEffects.LEVITATION, Math.round(80 * data.activeUtilityMultiplier() * durationMultiplier(data)), 0));
                    return true;
                })
                .orElseGet(() -> {
                    burst(level, player.position().add(0, 0.4D, 0), ParticleTypes.END_ROD, 22, 0.5D, 0.03D);
                    player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, Math.round(60 * data.activeUtilityMultiplier() * durationMultiplier(data)), 0));
                    return true;
                });
    }
}
