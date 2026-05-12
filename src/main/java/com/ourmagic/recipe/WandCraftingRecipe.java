package com.ourmagic.recipe;

import com.google.gson.JsonObject;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.StaffData;
import com.ourmagic.wand.WandData;
import com.ourmagic.wand.WandMaterialModifier;
import com.ourmagic.wand.WandModifier;
import com.ourmagic.wand.WandTemplates;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
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
import java.util.Optional;

public class WandCraftingRecipe extends CustomRecipe {
    public WandCraftingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (matchesUpgrade(container)) {
            return true;
        }

        if (matchesStaffCreation(container)) {
            return true;
        }

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

        ItemStack staffCreation = assembleStaffCreation(container);
        if (!staffCreation.isEmpty()) {
            return staffCreation;
        }

        return WandTemplates.applyRandom(new ItemStack(ModItems.WAND.get()), RandomSource.create());
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
        ItemStack staff = ItemStack.EMPTY;
        String modifierKey = null;
        int itemCount = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            itemCount++;
            if (isWand(stack) && wand.isEmpty()) {
                wand = stack;
            } else if (isStaff(stack) && staff.isEmpty()) {
                staff = stack;
            } else if (modifierKey == null) {
                // Check for leveled modifier
                Optional<WandModifier> leveledMod = WandModifier.fromItem(stack);
                if (leveledMod.isPresent()) {
                    modifierKey = leveledMod.get().key();
                } else {
                    // Check for material modifier
                    Optional<WandMaterialModifier> materialMod = WandMaterialModifier.fromItem(stack);
                    if (materialMod.isPresent()) {
                        modifierKey = materialMod.get().key();
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }

        if (itemCount != 2 || modifierKey == null) {
            return false;
        }

        if (!wand.isEmpty()) {
            return WandData.read(wand).canAddWandModifier(modifierKey);
        } else if (!staff.isEmpty()) {
            return StaffData.read(staff).canAddStaffModifier(modifierKey);
        }

        return false;
    }

    private static ItemStack assembleUpgrade(CraftingContainer container) {
        ItemStack wand = ItemStack.EMPTY;
        ItemStack staff = ItemStack.EMPTY;
        String modifierKey = null;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (isWand(stack)) {
                wand = stack;
            } else if (isStaff(stack)) {
                staff = stack;
            } else if (modifierKey == null) {
                // Check for leveled modifier
                Optional<WandModifier> leveledMod = WandModifier.fromItem(stack);
                if (leveledMod.isPresent()) {
                    modifierKey = leveledMod.get().key();
                } else {
                    // Check for material modifier
                    Optional<WandMaterialModifier> materialMod = WandMaterialModifier.fromItem(stack);
                    if (materialMod.isPresent()) {
                        modifierKey = materialMod.get().key();
                    }
                }
            }
        }

        if (modifierKey == null) {
            return ItemStack.EMPTY;
        }

        if (!wand.isEmpty()) {
            ItemStack result = wand.copy();
            result.setCount(1);
            WandData data = WandData.read(result);
            if (!data.addWandModifier(modifierKey)) {
                return ItemStack.EMPTY;
            }
            data.save(result);
            return result;
        } else if (!staff.isEmpty()) {
            ItemStack result = staff.copy();
            result.setCount(1);
            StaffData data = StaffData.read(result);
            if (!data.addStaffModifier(modifierKey)) {
                return ItemStack.EMPTY;
            }
            data.save(result);
            return result;
        }

        return ItemStack.EMPTY;
    }

    private static boolean isWand(ItemStack stack) {
        return stack.is(ModItems.WAND.get()) || stack.is(ModItems.ADMIN_WAND.get());
    }

    private static boolean isStaff(ItemStack stack) {
        return stack.is(ModItems.STAFF.get());
    }

    private static boolean matchesStaffCreation(CraftingContainer container) {
        int logs = 0;
        int netherStars = 0;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof net.minecraft.world.level.block.Block && net.minecraft.tags.ItemTags.LOGS.contains(stack.getItem())) {
                logs++;
            } else if (stack.is(Items.NETHER_STAR)) {
                netherStars++;
            } else {
                return false;
            }
        }

        return logs >= 1 && netherStars == 1;
    }

    private static ItemStack assembleStaffCreation(CraftingContainer container) {
        boolean hasLogs = false;
        boolean hasNetherStar = false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.getItem() instanceof net.minecraft.world.level.block.Block && net.minecraft.tags.ItemTags.LOGS.contains(stack.getItem())) {
                hasLogs = true;
            } else if (stack.is(Items.NETHER_STAR)) {
                hasNetherStar = true;
            }
        }

        if (!hasLogs || !hasNetherStar) {
            return ItemStack.EMPTY;
        }

        ItemStack result = new ItemStack(ModItems.STAFF.get());
        StaffData data = StaffData.createNew();
        data.save(result);
        return result;
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
