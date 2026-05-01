package com.ourmagic.recipe;

import com.ourmagic.OurMagic;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeSerializers {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, OurMagic.MOD_ID);

    public static final RegistryObject<RecipeSerializer<WandCraftingRecipe>> WAND = SERIALIZERS.register("wand", WandCraftingRecipe.Serializer::new);

    private ModRecipeSerializers() {
    }

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
