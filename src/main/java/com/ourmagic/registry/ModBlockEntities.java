package com.ourmagic.registry;

import com.ourmagic.OurMagic;
import com.ourmagic.block.entity.CreativeRfGeneratorBlockEntity;
import com.ourmagic.block.entity.MagicAccumulatorBlockEntity;
import com.ourmagic.block.entity.MagicBatteryBlockEntity;
import com.ourmagic.block.entity.MagicFlowConverterBlockEntity;
import com.ourmagic.block.entity.WardCamouflageBlockEntity;
import com.ourmagic.block.entity.WardStoneBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, OurMagic.MOD_ID);

    public static final RegistryObject<BlockEntityType<WardStoneBlockEntity>> WARD_STONE = BLOCK_ENTITIES.register("ward_stone",
            () -> BlockEntityType.Builder.of(WardStoneBlockEntity::new, ModBlocks.WARD_STONE.get()).build(null));
    public static final RegistryObject<BlockEntityType<MagicFlowConverterBlockEntity>> MAGIC_FLOW_CONVERTER = BLOCK_ENTITIES.register("magic_flow_converter",
            () -> BlockEntityType.Builder.of(MagicFlowConverterBlockEntity::new, ModBlocks.MAGIC_FLOW_CONVERTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<MagicBatteryBlockEntity>> MAGIC_BATTERY = BLOCK_ENTITIES.register("magic_battery",
            () -> BlockEntityType.Builder.of(MagicBatteryBlockEntity::new, ModBlocks.MAGIC_BATTERY.get()).build(null));
    public static final RegistryObject<BlockEntityType<MagicAccumulatorBlockEntity>> MAGIC_ACCUMULATOR = BLOCK_ENTITIES.register("magic_accumulator",
            () -> BlockEntityType.Builder.of(MagicAccumulatorBlockEntity::new,
                    ModBlocks.ARCANE_ACCUMULATOR.get(),
                    ModBlocks.FIRE_ACCUMULATOR.get(),
                    ModBlocks.WATER_ACCUMULATOR.get(),
                    ModBlocks.EARTH_ACCUMULATOR.get(),
                    ModBlocks.LIFE_ACCUMULATOR.get(),
                    ModBlocks.STORM_ACCUMULATOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<CreativeRfGeneratorBlockEntity>> CREATIVE_RF_GENERATOR = BLOCK_ENTITIES.register("creative_rf_generator",
            () -> BlockEntityType.Builder.of(CreativeRfGeneratorBlockEntity::new, ModBlocks.CREATIVE_RF_GENERATOR.get()).build(null));
    public static final RegistryObject<BlockEntityType<WardCamouflageBlockEntity>> WARD_CAMOUFLAGE = BLOCK_ENTITIES.register("ward_camouflage",
            () -> BlockEntityType.Builder.of(WardCamouflageBlockEntity::new, ModBlocks.WARD_CAMOUFLAGE.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
