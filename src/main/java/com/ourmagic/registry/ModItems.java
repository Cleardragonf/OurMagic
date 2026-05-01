package com.ourmagic.registry;

import com.ourmagic.OurMagic;
import com.ourmagic.item.SpellFocusItem;
import com.ourmagic.item.WandItem;
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

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
