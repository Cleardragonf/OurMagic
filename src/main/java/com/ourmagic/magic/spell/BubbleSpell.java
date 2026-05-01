package com.ourmagic.magic.spell;

import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BubbleSpell extends BaseSpell {
    public BubbleSpell() {
        super("bubble", 10, 30, UPGRADE_DURATION);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, Math.round(20 * 20 * data.activeUtilityMultiplier() * durationMultiplier(data)), 0));
        burst(level, player.position().add(0, 1.0D, 0), ParticleTypes.BUBBLE_POP, 45, 0.75D, 0.04D);
        ring(level, player.position().add(0, 0.2D, 0), ParticleTypes.BUBBLE, 1.0D, 24);
        return true;
    }
}
