package com.ourmagic.registry;

import com.ourmagic.OurMagic;
import com.ourmagic.entity.WarlockEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, OurMagic.MOD_ID);

    public static final RegistryObject<EntityType<WarlockEntity>> WARLOCK = ENTITIES.register("warlock",
            () -> EntityType.Builder.of(WarlockEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build(OurMagic.MOD_ID + ":warlock"));

    private ModEntities() {
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}
