package com.ourmagic.magic.spell;

import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class RegenerateSpell extends BaseSpell {
    public RegenerateSpell() {
        super("regenerate", 35, 100, UPGRADE_RANGE);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Math.round(160 * data.activeUtilityMultiplier() * durationMultiplier(data)), 1));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Math.round(120 * data.activeUtilityMultiplier() * durationMultiplier(data)), 0));
        double radius = 4.0D * (rangeMultiplier(data) - 1.0F);
        if (radius > 0) {
            for (Player target : level.getEntitiesOfClass(Player.class, new AABB(player.blockPosition()).inflate(radius))) {
                if (target != player) {
                    target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Math.round(120 * data.activeUtilityMultiplier() * durationMultiplier(data)), 0));
                    burst(level, target.position().add(0, 1.0D, 0), ParticleTypes.TOTEM_OF_UNDYING, 25, 0.6D, 0.06D);
                }
            }
        }
        burst(level, player.position().add(0, 1.0D, 0), ParticleTypes.TOTEM_OF_UNDYING, 55, 0.8D, 0.08D);
        ring(level, player.position().add(0, 0.2D, 0), ParticleTypes.HAPPY_VILLAGER, 1.2D, 36);
        return true;
    }
}
