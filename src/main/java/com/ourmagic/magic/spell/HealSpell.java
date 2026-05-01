package com.ourmagic.magic.spell;

import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class HealSpell extends BaseSpell {
    public HealSpell() {
        super("heal", 20, 60, UPGRADE_RANGE);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        player.heal(6.0F * data.power() * data.activeUtilityMultiplier());
        double radius = 4.0D * (rangeMultiplier(data) - 1.0F);
        if (radius > 0) {
            for (Player target : level.getEntitiesOfClass(Player.class, new AABB(player.blockPosition()).inflate(radius))) {
                if (target != player) {
                    target.heal(4.0F * data.power() * data.activeUtilityMultiplier());
                    burst(level, target.position().add(0, 1.1D, 0), ParticleTypes.HEART, 5, 0.45D, 0.02D);
                }
            }
        }
        burst(level, player.position().add(0, 1.1D, 0), ParticleTypes.HEART, 8, 0.55D, 0.02D);
        ring(level, player.position().add(0, 0.3D, 0), ParticleTypes.HAPPY_VILLAGER, 1.0D, 28);
        return true;
    }
}
