package com.ourmagic.magic.spell;

import com.ourmagic.wand.WandData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class FireSpell extends BaseSpell {
    public FireSpell() {
        super("fire", 12, 20);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        HitResult hit = raycast(player, 18 * data.power());
        if (!(hit instanceof BlockHitResult blockHit)) {
            return false;
        }
        beam(player, hit.getLocation(), ParticleTypes.FLAME);
        BlockPos pos = blockHit.getBlockPos().relative(blockHit.getDirection());
        if (!level.isEmptyBlock(pos)) {
            return false;
        }
        level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
        burst(level, pos.getCenter(), ParticleTypes.FLAME, 20, 0.35D, 0.02D);
        return true;
    }
}
