package io.github.mortuusars.exposure.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.mortuusars.exposure.Exposure;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.NotNull;

public class PhotographAgingRecipe extends AbstractNbtTransferringRecipe{
    public PhotographAgingRecipe(Identifier id, Ingredient transferIngredient,
                                 DefaultedList<Ingredient> ingredients, ItemStack result) {
        super(id, transferIngredient, ingredients, result);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Exposure.RecipeSerializers.PHOTOGRAPH_AGING.get();
    }

    public static class Serializer implements RecipeSerializer<PhotographAgingRecipe> {
        @Override
        public @NotNull PhotographAgingRecipe read(Identifier recipeId, JsonObject serializedRecipe) {
            Ingredient photographIngredient = Ingredient.fromJson(JsonHelper.getElement(serializedRecipe, "photograph"));
            DefaultedList<Ingredient> ingredients = getIngredients(JsonHelper.getArray(serializedRecipe, "ingredients"));
            ItemStack result = ShapedRecipe.outputFromJson(JsonHelper.getObject(serializedRecipe, "result"));

            if (photographIngredient.isEmpty())
                throw new JsonParseException("Recipe should have 'photograph' ingredient.");

            return new PhotographAgingRecipe(recipeId, photographIngredient, ingredients, result);
        }

        @Override
        public @NotNull PhotographAgingRecipe read(Identifier recipeId, PacketByteBuf buffer) {
            Ingredient transferredIngredient = Ingredient.fromPacket(buffer);
            int ingredientsCount = buffer.readVarInt();
            DefaultedList<Ingredient> ingredients = DefaultedList.ofSize(ingredientsCount, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.fromPacket(buffer));
            ItemStack result = buffer.readItemStack();

            return new PhotographAgingRecipe(recipeId, transferredIngredient, ingredients, result);
        }

        @Override
        public void write(PacketByteBuf buffer, PhotographAgingRecipe recipe) {
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
