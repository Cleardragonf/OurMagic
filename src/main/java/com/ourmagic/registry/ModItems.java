package com.ourmagic.registry;

import com.ourmagic.OurMagic;
import com.ourmagic.item.GrimoireItem;
import com.ourmagic.item.SpellFocusItem;
import com.ourmagic.item.WandItem;
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

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
