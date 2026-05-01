package com.ourmagic.magic.spell;

import com.ourmagic.wand.WandData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class GatherSpell extends BaseSpell {
    public GatherSpell() {
        super("gather", 15, 30);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        double radius = 10 * data.power() * data.activeUtilityMultiplier();
        ring(level, player.position().add(0, 0.25D, 0), ParticleTypes.ENCHANT, radius * 0.35D, 48);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(radius))) {
            beam(player, item.position().add(0, 0.25D, 0), ParticleTypes.ENCHANT);
            item.setDeltaMovement(player.position().subtract(item.position()).normalize().scale(0.65D));
            item.hurtMarked = true;
        }
        return true;
    }
}
