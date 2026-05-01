package com.ourmagic.magic.spell;

import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BlindSpell extends BaseSpell {
    public BlindSpell() {
        super("blind", 15, 35, UPGRADE_DURATION, UPGRADE_CHAINING);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        return castOnLivingTarget(level, player, data, 18 * data.power(), ParticleTypes.SQUID_INK, (target, targetPos) -> {
            beam(player, targetPos, ParticleTypes.SMOKE);
            applyBlind(level, target, data);
        });
    }

    private void applyBlind(Level level, LivingEntity target, WandData data) {
        burst(level, target.position().add(0, target.getBbHeight() * 0.75D, 0), ParticleTypes.SQUID_INK, 25, 0.45D, 0.02D);
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.round(100 * data.activeUtilityMultiplier() * durationMultiplier(data)), 0));
    }
}
