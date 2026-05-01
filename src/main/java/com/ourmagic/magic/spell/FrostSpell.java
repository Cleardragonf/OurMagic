package com.ourmagic.magic.spell;

import com.ourmagic.wand.WandData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;

public class FrostSpell extends BaseSpell {
    public FrostSpell() {
        super("frost", 14, 24, UPGRADE_DURATION);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        raycastEntity(player, 18 * data.power(), entity -> entity instanceof LivingEntity)
                .ifPresent(hit -> ((LivingEntity) hit.getEntity()).addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.round(100 * data.activeUtilityMultiplier() * durationMultiplier(data)), 2)));
        HitResult hit = raycast(player, 18 * data.power());
        beam(player, hit.getLocation(), ParticleTypes.SNOWFLAKE);
        burst(level, hit.getLocation(), ParticleTypes.SNOWFLAKE, 35, 0.7D, 0.04D);
        BlockPos center = BlockPos.containing(hit.getLocation());
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 0, 1))) {
            if (level.getBlockState(pos).is(Blocks.WATER)) {
                level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
            }
        }
        return true;
    }
}
