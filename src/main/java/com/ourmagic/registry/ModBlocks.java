package com.ourmagic.registry;

import com.ourmagic.OurMagic;
import com.ourmagic.block.CreativeRfGeneratorBlock;
import com.ourmagic.block.MagicFlowConverterBlock;
import com.ourmagic.block.TemporaryShieldBlock;
import com.ourmagic.block.WardBoundaryBlock;
import com.ourmagic.block.WardCamouflageBlock;
import com.ourmagic.block.WardPerimeterStoneBlock;
import com.ourmagic.block.WardStoneBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, OurMagic.MOD_ID);

    public static final RegistryObject<Block> TEMPORARY_SHIELD = BLOCKS.register("temporary_shield",
            () -> new TemporaryShieldBlock(BlockBehaviour.Properties.copy(Blocks.LIGHT_BLUE_STAINED_GLASS)
                    .strength(-1.0F, 3600000.0F)
                    .noOcclusion()
                    .lightLevel(state -> 4)));
    public static final RegistryObject<Block> WARD_STONE = BLOCKS.register("ward_stone",
            () -> new WardStoneBlock(BlockBehaviour.Properties.copy(Blocks.CRYING_OBSIDIAN)
                    .strength(5.0F, 1200.0F)
                    .lightLevel(state -> 6)));
    public static final RegistryObject<Block> MAGIC_FLOW_CONVERTER = BLOCKS.register("magic_flow_converter",
            () -> new MagicFlowConverterBlock(BlockBehaviour.Properties.copy(Blocks.LODESTONE)
                    .strength(4.0F, 18.0F)
                    .lightLevel(state -> 5)));
    public static final RegistryObject<Block> CREATIVE_RF_GENERATOR = BLOCKS.register("creative_rf_generator",
            () -> new CreativeRfGeneratorBlock(BlockBehaviour.Properties.copy(Blocks.REDSTONE_BLOCK)
                    .strength(4.0F, 18.0F)
                    .lightLevel(state -> 12)));
    public static final RegistryObject<Block> WARD_PERIMETER_STONE = BLOCKS.register("ward_perimeter_stone",
            () -> new WardPerimeterStoneBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_BRICKS)
                    .strength(3.5F, 12.0F)
                    .lightLevel(state -> 2)));
    public static final RegistryObject<Block> WARD_BOUNDARY = BLOCKS.register("ward_boundary",
            () -> new WardBoundaryBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(-1.0F, 3600000.0F)
                    .forceSolidOn()
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .noLootTable()));
    public static final RegistryObject<Block> WARD_CAMOUFLAGE = BLOCKS.register("ward_camouflage",
            () -> new WardCamouflageBlock(BlockBehaviour.Properties.copy(Blocks.STONE)
                    .strength(-1.0F, 3600000.0F)
                    .forceSolidOn()
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .noLootTable()));

    private ModBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
