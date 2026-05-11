package com.ourmagic.registry;

import com.ourmagic.OurMagic;
import com.ourmagic.ui.PlayerUpgradeMenu;
import com.ourmagic.ui.SpellcraftMenu;
import com.ourmagic.ui.WandMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, OurMagic.MOD_ID);

    public static final RegistryObject<MenuType<WandMenu>> WAND = MENUS.register("wand", () -> IForgeMenuType.create(WandMenu::new));
    public static final RegistryObject<MenuType<SpellcraftMenu>> SPELLCRAFT = MENUS.register("spellcraft", () -> IForgeMenuType.create(SpellcraftMenu::new));
    public static final RegistryObject<MenuType<PlayerUpgradeMenu>> PLAYER_UPGRADES = MENUS.register("player_upgrades", () -> IForgeMenuType.create(PlayerUpgradeMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
