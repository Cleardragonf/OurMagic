package com.ourmagic.registry;

import com.ourmagic.OurMagic;
import com.ourmagic.wand.WandTemplates;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OurMagic.MOD_ID);

    public static final RegistryObject<CreativeModeTab> OURMAGIC = TABS.register("ourmagic", () -> CreativeModeTab.builder()
            .title(Component.literal("OurMagic"))
            .icon(() -> WandTemplates.applyRandom(new ItemStack(ModItems.WAND.get()), RandomSource.create(1L)))
            .displayItems((parameters, output) -> {
                output.accept(WandTemplates.applyRandom(new ItemStack(ModItems.WAND.get()), RandomSource.create(2L)));
                output.accept(WandTemplates.applyAdmin(new ItemStack(ModItems.ADMIN_WAND.get())));
                output.accept(ModItems.SPELL_FOCUS.get());
                output.accept(ModItems.GRIMOIRE.get());
                output.accept(ModItems.GUIDE_BOOK.get());
                output.accept(ModItems.WARD_TUNER.get());
                output.accept(ModItems.LIGHTNING_TALISMAN.get());
                output.accept(ModItems.ANCHOR_TALISMAN.get());
                output.accept(ModItems.SEEKER_TALISMAN.get());
                output.accept(ModItems.WAYPOINT_TALISMAN.get());
                output.accept(ModItems.MASON_TALISMAN.get());
                output.accept(ModItems.PULSE_TALISMAN.get());
                output.accept(ModItems.STORMCALL_TALISMAN.get());
                output.accept(ModItems.CONFLUX_TALISMAN.get());
                output.accept(ModItems.SANCTUARY_TALISMAN.get());
                output.accept(ModItems.BEACON_TALISMAN.get());
                output.accept(ModItems.MAGNET_TALISMAN.get());
                output.accept(ModItems.TIDE_TALISMAN.get());
                output.accept(ModItems.TEMPORARY_SHIELD.get());
                output.accept(ModItems.WARD_STONE.get());
                output.accept(ModItems.MAGIC_FLOW_CONVERTER.get());
                output.accept(ModItems.CREATIVE_RF_GENERATOR.get());
                output.accept(ModItems.WARD_PERIMETER_STONE.get());
                output.accept(ModItems.WARLOCK_SPAWN_EGG.get());
            })
            .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
