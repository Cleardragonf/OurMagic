package com.ourmagic.registry;

import com.ourmagic.OurMagic;
import com.ourmagic.block.CreativeRfGeneratorBlock;
import com.ourmagic.block.MagicAccumulatorBlock;
import com.ourmagic.block.MagicBatteryBlock;
import com.ourmagic.block.MagicFlowConverterBlock;
import com.ourmagic.block.MagicRelayBlock;
import com.ourmagic.block.TeleportPortalBlock;
import com.ourmagic.block.TemporaryShieldBlock;
import com.ourmagic.block.WardBoundaryBlock;
import com.ourmagic.block.WardBlock;
import com.ourmagic.block.WardCamouflageBlock;
import com.ourmagic.block.WardPerimeterStoneBlock;
import com.ourmagic.block.WardStoneBlock;
import com.ourmagic.magic.energy.MagicEnergyType;
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
                    .noOcclusion()
                    .lightLevel(state -> 5)));
    public static final RegistryObject<Block> MAGIC_BATTERY = BLOCKS.register("magic_battery",
            () -> new MagicBatteryBlock(BlockBehaviour.Properties.copy(Blocks.RESPAWN_ANCHOR)
                    .strength(8.0F, 1200.0F)
                    .lightLevel(state -> 7)));
    public static final RegistryObject<Block> MAGIC_RELAY = BLOCKS.register("magic_relay",
            () -> new MagicRelayBlock(BlockBehaviour.Properties.copy(Blocks.LODESTONE)
                    .strength(4.0F, 18.0F)
                    .noOcclusion()
                    .lightLevel(state -> 6)));
    public static final RegistryObject<Block> ARCANE_ACCUMULATOR = BLOCKS.register("arcane_accumulator",
            () -> new MagicAccumulatorBlock(MagicEnergyType.ARCANE, BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                    .strength(3.0F, 9.0F)
                    .lightLevel(state -> 8)));
    public static final RegistryObject<Block> DARK_ACCUMULATOR = BLOCKS.register("dark_accumulator",
            () -> new MagicAccumulatorBlock(MagicEnergyType.DARK, BlockBehaviour.Properties.copy(Blocks.SCULK)
                    .strength(3.0F, 9.0F)
                    .lightLevel(state -> 4)));
    public static final RegistryObject<Block> FIRE_ACCUMULATOR = BLOCKS.register("fire_accumulator",
            () -> new MagicAccumulatorBlock(MagicEnergyType.FIRE, BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                    .strength(3.0F, 9.0F)
                    .lightLevel(state -> 9)));
    public static final RegistryObject<Block> WATER_ACCUMULATOR = BLOCKS.register("water_accumulator",
            () -> new MagicAccumulatorBlock(MagicEnergyType.WATER, BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                    .strength(3.0F, 9.0F)
                    .lightLevel(state -> 7)));
    public static final RegistryObject<Block> EARTH_ACCUMULATOR = BLOCKS.register("earth_accumulator",
            () -> new MagicAccumulatorBlock(MagicEnergyType.EARTH, BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                    .strength(3.0F, 9.0F)
                    .lightLevel(state -> 5)));
    public static final RegistryObject<Block> LIFE_ACCUMULATOR = BLOCKS.register("life_accumulator",
            () -> new MagicAccumulatorBlock(MagicEnergyType.LIFE, BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                    .strength(3.0F, 9.0F)
                    .lightLevel(state -> 7)));
    public static final RegistryObject<Block> STORM_ACCUMULATOR = BLOCKS.register("storm_accumulator",
            () -> new MagicAccumulatorBlock(MagicEnergyType.STORM, BlockBehaviour.Properties.copy(Blocks.AMETHYST_BLOCK)
                    .strength(3.0F, 9.0F)
                    .lightLevel(state -> 10)));
    public static final RegistryObject<Block> CREATIVE_RF_GENERATOR = BLOCKS.register("creative_rf_generator",
            () -> new CreativeRfGeneratorBlock(BlockBehaviour.Properties.copy(Blocks.REDSTONE_BLOCK)
                    .strength(4.0F, 18.0F)
                    .noOcclusion()
                    .lightLevel(state -> 12)));
    public static final RegistryObject<Block> WARD_PERIMETER_STONE = BLOCKS.register("ward_perimeter_stone",
            () -> new WardPerimeterStoneBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_BRICKS)
                    .strength(3.5F, 12.0F)
                    .lightLevel(state -> 2)));
    public static final RegistryObject<Block> TELEPORT_PORTAL = BLOCKS.register("teleport_portal",
            () -> new TeleportPortalBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_TILES)
                    .strength(3.5F, 12.0F)
                    .noOcclusion()
                    .lightLevel(state -> 7)));
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
    public static final RegistryObject<Block> WARD = BLOCKS.register("ward",
            () -> new WardBlock(BlockBehaviour.Properties.copy(Blocks.AIR)
                    .air()
                    .noCollission()
                    .noOcclusion()
                    .replaceable()
                    .lightLevel(state -> state.getValue(WardBlock.LEVEL))
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
