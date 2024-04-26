package io.github.mortuusars.exposure.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.mortuusars.exposure.Exposure;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class FilmDevelopingRecipe extends AbstractNbtTransferringRecipe {
    public FilmDevelopingRecipe(Identifier id, Ingredient filmIngredient, DefaultedList<Ingredient> ingredients, ItemStack result) {
        super(id, filmIngredient, ingredients, result);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Exposure.RecipeSerializers.FILM_DEVELOPING.get();
    }

    @Override
    public @NotNull DefaultedList<ItemStack> getRemainder(@NotNull RecipeInputInventory container) {
        DefaultedList<ItemStack> remainingItems = super.getRemainder(container);

        for (int i = 0; i < container.size(); ++i) {
            ItemStack item = container.getStack(i);
            if (item.getItem() instanceof PotionItem) {
                remainingItems.set(i, new ItemStack(Items.GLASS_BOTTLE));
            }
            else if (item.getItem().hasRecipeRemainder()) {
                remainingItems.set(i, new ItemStack(Objects.requireNonNull(item.getItem().getRecipeRemainder())));
            }
        }

        return remainingItems;
    }

    public static class Serializer implements RecipeSerializer<FilmDevelopingRecipe> {
        @Override
        public @NotNull FilmDevelopingRecipe read(Identifier recipeId, JsonObject serializedRecipe) {
            Ingredient filmIngredient = Ingredient.fromJson(JsonHelper.getElement(serializedRecipe, "film"));
            DefaultedList<Ingredient> ingredients = getIngredients(JsonHelper.getArray(serializedRecipe, "ingredients"));
            ItemStack result = ShapedRecipe.outputFromJson(JsonHelper.getObject(serializedRecipe, "result"));

            if (filmIngredient.isEmpty())
                throw new JsonParseException("Recipe should have 'film' ingredient.");

            return new FilmDevelopingRecipe(recipeId, filmIngredient, ingredients, result);
        }

        @Override
        public @NotNull FilmDevelopingRecipe read(Identifier recipeId, PacketByteBuf buffer) {
            Ingredient transferredIngredient = Ingredient.fromPacket(buffer);
            int ingredientsCount = buffer.readVarInt();
            DefaultedList<Ingredient> ingredients = DefaultedList.ofSize(ingredientsCount, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.fromPacket(buffer));
            ItemStack result = buffer.readItemStack();

            return new FilmDevelopingRecipe(recipeId, transferredIngredient, ingredients, result);
        }

        @Override
        public void write(PacketByteBuf buffer, FilmDevelopingRecipe recipe) {
            recipe.getTransferIngredient().write(buffer);
            buffer.writeVarInt(recipe.getIngredients().size());
            for (Ingredient ingredient : recipe.getIngredients()) {
                ingredient.write(buffer);
            }
            buffer.writeItemStack(recipe.getResult());
        }

        private DefaultedList<Ingredient> getIngredients(JsonArray jsonArray) {
            DefaultedList<Ingredient> ingredients = DefaultedList.of();

            for (int i = 0; i < jsonArray.size(); ++i) {
                Ingredient ingredient = Ingredient.fromJson(jsonArray.get(i));
                if (!ingredient.isEmpty())
                    ingredients.add(ingredient);
            }

            if (ingredients.isEmpty())
                throw new JsonParseException("No ingredients for a recipe.");
            else if (ingredients.size() > 3 * 3)
                throw new JsonParseException("Too many ingredients for a recipe. The maximum is 9.");
            return ingredients;
        }
    }
}
