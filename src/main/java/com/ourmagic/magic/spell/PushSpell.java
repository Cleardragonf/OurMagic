package com.ourmagic.magic.spell;

import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PushSpell extends BaseSpell {
    public PushSpell() {
        super("push", 12, 18, UPGRADE_MULTISTRIKE);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        boolean pushed = false;
        for (int i = 0; i < multistrikeCasts(data); i++) {
            pushed |= raycastEntity(player, 18 * data.power(), entity -> entity instanceof LivingEntity)
                    .map(hit -> {
                        beam(player, hit.getLocation(), ParticleTypes.CLOUD);
                        ring(level, hit.getEntity().position().add(0, 0.7D, 0), ParticleTypes.POOF, 1.0D, 20);
                        hit.getEntity().push(player.getLookAngle().x * 1.7D * data.power() * data.activeUtilityMultiplier(), 0.45D, player.getLookAngle().z * 1.7D * data.power() * data.activeUtilityMultiplier());
                        hit.getEntity().hurtMarked = true;
                        return true;
                    })
                    .orElse(false);
        }
        return pushed;
    }
}
