package com.ourmagic.magic.spell.payloads;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.Set;

public class TransmutePayload implements PayloadEffect {
    private static final Map<Block, Block> TRANSMUTES = Map.ofEntries(
            Map.entry(Blocks.STONE, Blocks.MOSS_BLOCK),
            Map.entry(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE),
            Map.entry(Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS),
            Map.entry(Blocks.DIRT, Blocks.GRASS_BLOCK),
            Map.entry(Blocks.SAND, Blocks.GLASS),
            Map.entry(Blocks.GRAVEL, Blocks.CLAY),
            Map.entry(Blocks.WATER, Blocks.ICE),
            Map.entry(Blocks.LAVA, Blocks.OBSIDIAN)
    );

    @Override
    public ParticleOptions particle(SpellBuildContext context) {
        return ParticleTypes.HAPPY_VILLAGER;
    }

    @Override
    public Set<String> supportedUpgrades(SpellBuildContext context) {
        return Set.of(Spell.UPGRADE_RADIUS);
    }

    @Override
    public boolean apply(SpellContext context, SpellTarget target) {
        BlockPos center = target.block().orElse(BlockPos.containing(target.position()));
        int radius = context.areaCast() ? Math.max(1, Math.round(context.radiusMultiplier())) : 0;
        boolean changed = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            Block block = context.level().getBlockState(pos).getBlock();
            Block replacement = TRANSMUTES.get(block);
            if (replacement != null) {
                context.level().setBlockAndUpdate(pos.immutable(), replacement.defaultBlockState());
                changed = true;
            }
        }
        if (changed) {
            context.burst(center.getCenter(), ParticleTypes.HAPPY_VILLAGER, 24, 0.65D + radius, 0.03D);
        }
        return changed;
    }
}
