package com.ourmagic.recipe;

import com.google.gson.JsonObject;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.WandData;
import com.ourmagic.wand.WandModifier;
import com.ourmagic.wand.WandTemplates;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class WandCraftingRecipe extends CustomRecipe {
    public WandCraftingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (matchesUpgrade(container)) {
            return true;
        }

        return matchesBasicWand(container);
    }

    private static boolean matchesBasicWand(CraftingContainer container) {
        int sticks = 0;
        int crystals = 0;
        int dust = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.STICK)) {
                sticks++;
            } else if (stack.is(Items.AMETHYST_SHARD)) {
                crystals++;
            } else if (stack.is(Items.GLOWSTONE_DUST)) {
                dust++;
            } else {
                return false;
            }
        }

        return sticks == 2 && crystals == 1 && dust == 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack upgrade = assembleUpgrade(container);
        if (!upgrade.isEmpty()) {
            return upgrade;
        }

        if (!matchesBasicWand(container)) {
            return ItemStack.EMPTY;
        }

        return WandTemplates.applyRandom(new ItemStack(ModItems.WAND.get()), RandomSource.create(wandSeed(container)));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 4;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return WandTemplates.applyRandom(new ItemStack(ModItems.WAND.get()), RandomSource.create(3L));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(Items.STICK));
        ingredients.add(Ingredient.of(Items.STICK));
        ingredients.add(Ingredient.of(Items.AMETHYST_SHARD));
        ingredients.add(Ingredient.of(Items.GLOWSTONE_DUST));
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.WAND.get();
    }

    private static boolean matchesUpgrade(CraftingContainer container) {
        ItemStack wand = ItemStack.EMPTY;
        WandModifier modifier = null;
        int itemCount = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            itemCount++;
            if (isWand(stack) && wand.isEmpty()) {
                wand = stack;
            } else if (modifier == null && WandModifier.fromItem(stack).isPresent()) {
                modifier = WandModifier.fromItem(stack).get();
            } else {
                return false;
            }
        }

        return itemCount == 2 && !wand.isEmpty() && modifier != null && WandData.read(wand).canAddWandModifier(modifier.key());
    }

    private static ItemStack assembleUpgrade(CraftingContainer container) {
        ItemStack wand = ItemStack.EMPTY;
        WandModifier modifier = null;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (isWand(stack)) {
                wand = stack;
            } else {
                modifier = WandModifier.fromItem(stack).orElse(null);
            }
        }

        if (wand.isEmpty() || modifier == null) {
            return ItemStack.EMPTY;
        }

        ItemStack result = wand.copy();
        result.setCount(1);
        WandData data = WandData.read(result);
        if (!data.addWandModifier(modifier.key())) {
            return ItemStack.EMPTY;
        }
        data.save(result);
        return result;
    }

    private static boolean isWand(ItemStack stack) {
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
    }

    private static long wandSeed(CraftingContainer container) {
        long seed = 0x4F75724D61676963L;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            seed = seed * 31L + i;
            if (stack.isEmpty()) {
                continue;
            }
            seed = seed * 31L + BuiltInRegistries.ITEM.getKey(stack.getItem()).hashCode();
            seed = seed * 31L + stack.getCount();
            if (stack.hasTag()) {
                seed = seed * 31L + stack.getTag().hashCode();
            }
        }
        return seed;
    }

    public static class Serializer implements RecipeSerializer<WandCraftingRecipe> {
        @Override
        public WandCraftingRecipe fromJson(ResourceLocation recipeId, JsonObject serializedRecipe) {
            return new WandCraftingRecipe(recipeId, CraftingBookCategory.EQUIPMENT);
        }

        @Override
        public WandCraftingRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            return new WandCraftingRecipe(recipeId, CraftingBookCategory.EQUIPMENT);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, WandCraftingRecipe recipe) {
        }
    }
}
