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
    public static final RegistryObject<Item> TEMPORARY_SHIELD = ITEMS.register("temporary_shield", () -> new BlockItem(ModBlocks.TEMPORARY_SHIELD.get(), new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
