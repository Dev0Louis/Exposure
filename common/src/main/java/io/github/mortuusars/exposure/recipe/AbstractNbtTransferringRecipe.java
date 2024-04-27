package io.github.mortuusars.exposure.recipe;

import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNbtTransferringRecipe extends SpecialCraftingRecipe {
    private final ItemStack result;
    private final Ingredient transferIngredient;
    private final DefaultedList<Ingredient> ingredients;

    public AbstractNbtTransferringRecipe(Ingredient transferIngredient, DefaultedList<Ingredient> ingredients, ItemStack result) {
        super(CraftingRecipeCategory.MISC);
        this.transferIngredient = transferIngredient;
        this.ingredients = ingredients;
        this.result = result;
    }

    public @NotNull Ingredient getTransferIngredient() {
        return transferIngredient;
    }

    @Override
    public @NotNull DefaultedList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public @NotNull ItemStack getResult(DynamicRegistryManager registryAccess) {
        return getResult();
    }

    public @NotNull ItemStack getResult() {
        return result;
    }

    @Override
    public boolean matches(RecipeInputInventory container, World level) {
        if (getTransferIngredient().isEmpty() || ingredients.isEmpty())
            return false;

        List<Ingredient> unmatchedIngredients = new ArrayList<>(ingredients);
        unmatchedIngredients.add(0, getTransferIngredient());

        int itemsInCraftingGrid = 0;

        for (int i = 0; i < container.size(); i++) {
            ItemStack stack = container.getStack(i);
            if (!stack.isEmpty())
                itemsInCraftingGrid++;

            if (itemsInCraftingGrid > ingredients.size() + 1)
                return false;

            if (!unmatchedIngredients.isEmpty()) {
                for (int j = 0; j < unmatchedIngredients.size(); j++) {
                    if (unmatchedIngredients.get(j).test(stack)) {
                        unmatchedIngredients.remove(j);
                        break;
                    }
                }
            }
        }

        return unmatchedIngredients.isEmpty() && itemsInCraftingGrid == ingredients.size() + 1;
    }

    @Override
    public @NotNull ItemStack craft(RecipeInputInventory container, @NotNull DynamicRegistryManager registryAccess) {
        for (int index = 0; index < container.size(); index++) {
            ItemStack itemStack = container.getStack(index);

            if (getTransferIngredient().test(itemStack)) {
                return transferNbt(itemStack, getResult(registryAccess).copy());
            }
        }

        return getResult(registryAccess);
    }

    public @NotNull ItemStack transferNbt(ItemStack transferIngredientStack, ItemStack recipeResultStack) {
        @Nullable NbtCompound transferTag = transferIngredientStack.getNbt();
        if (transferTag != null) {
            if (recipeResultStack.getNbt() != null)
                recipeResultStack.getNbt().copyFrom(transferTag);
            else
                recipeResultStack.setNbt(transferTag.copy());
        }
        return recipeResultStack;
    }

    @Override
    public boolean fits(int width, int height) {
        return ingredients.size() <= width * height;
    }
}
