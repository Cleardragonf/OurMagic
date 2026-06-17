package com.ourmagic.registry;

import com.ourmagic.OurMagic;
import com.ourmagic.item.GuideBookItem;
import com.ourmagic.item.GrimoireItem;
import com.ourmagic.item.MagicLinkerItem;
import com.ourmagic.item.SpellFocusItem;
import com.ourmagic.item.WandItem;
import com.ourmagic.item.WardDiagramItem;
import com.ourmagic.item.WardTunerItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, OurMagic.MOD_ID);

    public static final RegistryObject<Item> WAND = ITEMS.register("wand", () -> new WandItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ADMIN_WAND = ITEMS.register("admin_wand", () -> new WandItem(new Item.Properties().stacksTo(1), true));
    public static final RegistryObject<Item> SPELL_FOCUS = ITEMS.register("spell_focus", () -> new SpellFocusItem(new Item.Properties()));
    public static final RegistryObject<Item> GRIMOIRE = ITEMS.register("grimoire", () -> new GrimoireItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> GUIDE_BOOK = ITEMS.register("guide_book", () -> new GuideBookItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> WARD_TUNER = ITEMS.register("ward_tuner", () -> new WardTunerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> WARD_DIAGRAM = ITEMS.register("ward_diagram", () -> new WardDiagramItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> MAGIC_LINKER = ITEMS.register("magic_linker", () -> new MagicLinkerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> LIGHTNING_TALISMAN = ITEMS.register("lightning_talisman", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> ANCHOR_TALISMAN = ITEMS.register("anchor_talisman", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SEEKER_TALISMAN = ITEMS.register("seeker_talisman", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WAYPOINT_TALISMAN = ITEMS.register("waypoint_talisman", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MASON_TALISMAN = ITEMS.register("mason_talisman", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PULSE_TALISMAN = ITEMS.register("pulse_talisman", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> STORMCALL_TALISMAN = ITEMS.register("stormcall_talisman", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CONFLUX_TALISMAN = ITEMS.register("conflux_talisman", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SANCTUARY_TALISMAN = ITEMS.register("sanctuary_talisman", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BEACON_TALISMAN = ITEMS.register("beacon_talisman", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MAGNET_TALISMAN = ITEMS.register("magnet_talisman", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TIDE_TALISMAN = ITEMS.register("tide_talisman", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TEMPORARY_SHIELD = ITEMS.register("temporary_shield", () -> new BlockItem(ModBlocks.TEMPORARY_SHIELD.get(), new Item.Properties()));
    public static final RegistryObject<Item> WARD_STONE = ITEMS.register("ward_stone", () -> new BlockItem(ModBlocks.WARD_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> MAGIC_FLOW_CONVERTER = ITEMS.register("magic_flow_converter", () -> new BlockItem(ModBlocks.MAGIC_FLOW_CONVERTER.get(), new Item.Properties()));
    public static final RegistryObject<Item> MAGIC_BATTERY = ITEMS.register("magic_battery", () -> new BlockItem(ModBlocks.MAGIC_BATTERY.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARCANE_ACCUMULATOR = ITEMS.register("arcane_accumulator", () -> new BlockItem(ModBlocks.ARCANE_ACCUMULATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> DARK_ACCUMULATOR = ITEMS.register("dark_accumulator", () -> new BlockItem(ModBlocks.DARK_ACCUMULATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> FIRE_ACCUMULATOR = ITEMS.register("fire_accumulator", () -> new BlockItem(ModBlocks.FIRE_ACCUMULATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> WATER_ACCUMULATOR = ITEMS.register("water_accumulator", () -> new BlockItem(ModBlocks.WATER_ACCUMULATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> EARTH_ACCUMULATOR = ITEMS.register("earth_accumulator", () -> new BlockItem(ModBlocks.EARTH_ACCUMULATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> LIFE_ACCUMULATOR = ITEMS.register("life_accumulator", () -> new BlockItem(ModBlocks.LIFE_ACCUMULATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> STORM_ACCUMULATOR = ITEMS.register("storm_accumulator", () -> new BlockItem(ModBlocks.STORM_ACCUMULATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> CREATIVE_RF_GENERATOR = ITEMS.register("creative_rf_generator", () -> new BlockItem(ModBlocks.CREATIVE_RF_GENERATOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> WARD_PERIMETER_STONE = ITEMS.register("ward_perimeter_stone", () -> new BlockItem(ModBlocks.WARD_PERIMETER_STONE.get(), new Item.Properties()));
    public static final RegistryObject<Item> WARLOCK_SPAWN_EGG = ITEMS.register("warlock_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.WARLOCK, 0x2A1734, 0xC66CFF, new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
