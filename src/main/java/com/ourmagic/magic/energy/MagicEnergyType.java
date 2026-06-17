package com.ourmagic.magic.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import java.util.Locale;

public enum MagicEnergyType {
    ARCANE("Arcane", 0x9D61FF),
    DARK("Dark", 0x4A1B5F),
    FIRE("Fire", 0xFF6A22),
    WATER("Water", 0x36A9FF),
    EARTH("Earth", 0x8B6F3E),
    LIFE("Life", 0x55C96B),
    STORM("Storm", 0xD8E6FF);

    private final String displayName;
    private final int color;

    MagicEnergyType(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public int color() {
        return color;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public int sourceValue(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        FluidState fluid = level.getFluidState(pos);
        return switch (this) {
            case ARCANE -> arcaneValue(state);
            case DARK -> darkValue(level, pos, state);
            case FIRE -> fireValue(state, fluid);
            case WATER -> waterValue(state, fluid);
            case EARTH -> earthValue(state);
            case LIFE -> lifeValue(state);
            case STORM -> stormValue(state);
        };
    }

    private static int darkValue(Level level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.SCULK) || state.is(Blocks.SCULK_CATALYST) || state.is(Blocks.SCULK_SHRIEKER)) {
            return 16;
        }
        if (state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL) || state.is(Blocks.WITHER_ROSE)) {
            return 10;
        }
        if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)) {
            return 5;
        }
        if (level.getMaxLocalRawBrightness(pos) <= 0) {
            return 1;
        }
        return 0;
    }

    private static int arcaneValue(BlockState state) {
        if (state.is(Blocks.AMETHYST_BLOCK) || state.is(Blocks.BUDDING_AMETHYST)) {
            return 18;
        }
        if (state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.ENCHANTING_TABLE)) {
            return 14;
        }
        if (state.is(Blocks.BOOKSHELF) || state.is(Blocks.CHISELED_BOOKSHELF)) {
            return 5;
        }
        return state.is(Blocks.LAPIS_BLOCK) ? 4 : 0;
    }

    private static int fireValue(BlockState state, FluidState fluid) {
        if (fluid.is(Fluids.LAVA) || fluid.is(Fluids.FLOWING_LAVA)) {
            return 16;
        }
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.MAGMA_BLOCK)) {
            return 10;
        }
        if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE)) {
            return 5;
        }
        return 0;
    }

    private static int waterValue(BlockState state, FluidState fluid) {
        if (fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER)) {
            return 10;
        }
        if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)) {
            return 8;
        }
        if (state.is(Blocks.KELP) || state.is(Blocks.SEAGRASS) || state.is(Blocks.SEA_LANTERN)) {
            return 5;
        }
        return 0;
    }

    private static int earthValue(BlockState state) {
        if (state.is(Blocks.DEEPSLATE) || state.is(Blocks.DEEPSLATE_BRICKS) || state.is(Blocks.TUFF)) {
            return 5;
        }
        if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE)) {
            return 3;
        }
        if (state.is(Blocks.IRON_ORE) || state.is(Blocks.COPPER_ORE) || state.is(Blocks.GOLD_ORE) || state.is(Blocks.DIAMOND_ORE)) {
            return 12;
        }
        return 0;
    }

    private static int lifeValue(BlockState state) {
        if (state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.FLOWERING_AZALEA) || state.is(Blocks.SPORE_BLOSSOM)) {
            return 8;
        }
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.FARMLAND) || state.is(Blocks.OAK_LEAVES) || state.is(Blocks.BIRCH_LEAVES)
                || state.is(Blocks.SPRUCE_LEAVES) || state.is(Blocks.JUNGLE_LEAVES) || state.is(Blocks.ACACIA_LEAVES) || state.is(Blocks.DARK_OAK_LEAVES)
                || state.is(Blocks.MANGROVE_LEAVES) || state.is(Blocks.CHERRY_LEAVES)) {
            return 4;
        }
        if (state.is(Blocks.WHEAT) || state.is(Blocks.CARROTS) || state.is(Blocks.POTATOES) || state.is(Blocks.BEETROOTS)) {
            return 6;
        }
        return 0;
    }

    private static int stormValue(BlockState state) {
        if (state.is(Blocks.LIGHTNING_ROD)) {
            return 18;
        }
        if (state.is(Blocks.COPPER_BLOCK) || state.is(Blocks.EXPOSED_COPPER) || state.is(Blocks.WEATHERED_COPPER) || state.is(Blocks.OXIDIZED_COPPER)) {
            return 6;
        }
        if (state.is(Blocks.REDSTONE_BLOCK) || state.is(Blocks.TARGET)) {
            return 5;
        }
        return 0;
    }
}
