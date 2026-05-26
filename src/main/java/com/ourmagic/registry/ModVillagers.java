package com.ourmagic.registry;

import com.google.common.collect.ImmutableSet;
import com.ourmagic.OurMagic;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModVillagers {
    private static final DeferredRegister<PoiType> POIS = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, OurMagic.MOD_ID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, OurMagic.MOD_ID);

    public static final RegistryObject<PoiType> WIZARD_POI = POIS.register("wizard_poi",
            () -> new PoiType(ImmutableSet.copyOf(Blocks.ENCHANTING_TABLE.getStateDefinition().getPossibleStates()), 1, 1));

    public static final RegistryObject<VillagerProfession> WIZARD = PROFESSIONS.register("wizard",
            () -> new VillagerProfession("wizard", ModVillagers::isWizardPoi, ModVillagers::isWizardPoi,
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.ENCHANTMENT_TABLE_USE));

    private ModVillagers() {
    }

    private static boolean isWizardPoi(Holder<PoiType> poi) {
        return poi.value() == WIZARD_POI.get();
    }

    public static void register(IEventBus bus) {
        POIS.register(bus);
        PROFESSIONS.register(bus);
    }
}
