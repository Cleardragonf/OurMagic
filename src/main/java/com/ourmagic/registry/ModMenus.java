package com.ourmagic.registry;

import com.ourmagic.OurMagic;
import com.ourmagic.ui.ArcaneQuarryMenu;
import com.ourmagic.ui.CrystalDriveBayMenu;
import com.ourmagic.ui.PlayerUpgradeMenu;
import com.ourmagic.ui.QuantumStorageMenu;
import com.ourmagic.ui.SpellcraftMenu;
import com.ourmagic.ui.WandMenu;
import com.ourmagic.ui.WardCrafterMenu;
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
    public static final RegistryObject<MenuType<WardCrafterMenu>> WARD_CRAFTER = MENUS.register("ward_crafter", () -> IForgeMenuType.create(WardCrafterMenu::new));
    public static final RegistryObject<MenuType<PlayerUpgradeMenu>> PLAYER_UPGRADES = MENUS.register("player_upgrades", () -> IForgeMenuType.create(PlayerUpgradeMenu::new));
    public static final RegistryObject<MenuType<CrystalDriveBayMenu>> CRYSTAL_DRIVE_BAY = MENUS.register("crystal_drive_bay", () -> IForgeMenuType.create(CrystalDriveBayMenu::new));
    public static final RegistryObject<MenuType<ArcaneQuarryMenu>> ARCANE_QUARRY = MENUS.register("arcane_quarry", () -> IForgeMenuType.create(ArcaneQuarryMenu::new));
    public static final RegistryObject<MenuType<QuantumStorageMenu>> QUANTUM_STORAGE = MENUS.register("quantum_storage", () -> IForgeMenuType.create(QuantumStorageMenu::new));

    private ModMenus() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
