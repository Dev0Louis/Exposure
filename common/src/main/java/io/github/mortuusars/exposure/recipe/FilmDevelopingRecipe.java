package io.github.mortuusars.exposure.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
    public FilmDevelopingRecipe(Ingredient filmIngredient, DefaultedList<Ingredient> ingredients, ItemStack result) {
        super(filmIngredient, ingredients, result);
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

        Codec<DefaultedList<Ingredient>> DEFAULTED_INGREDIENT_LIST = RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(Ingredient.DISALLOW_EMPTY_CODEC).fieldOf("ingredients").forGetter(ingredients -> ingredients.delegate),
                Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("defaultIngredient").forGetter(ingredients -> ingredients.initialElement)
        ).apply(instance, DefaultedList::new));

        Codec<FilmDevelopingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("transferIngredient").forGetter(FilmDevelopingRecipe::getTransferIngredient),
                DEFAULTED_INGREDIENT_LIST.fieldOf("ingredients").forGetter(FilmDevelopingRecipe::getIngredients),
                ItemStack.CODEC.fieldOf("result").forGetter(FilmDevelopingRecipe::getResult)

        ).apply(
                instance, FilmDevelopingRecipe::new
        ));

        @Override
        public Codec<FilmDevelopingRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull FilmDevelopingRecipe read(PacketByteBuf buffer) {
            Ingredient transferredIngredient = Ingredient.fromPacket(buffer);
            int ingredientsCount = buffer.readVarInt();
            DefaultedList<Ingredient> ingredients = DefaultedList.ofSize(ingredientsCount, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.fromPacket(buffer));
            ItemStack result = buffer.readItemStack();

            return new FilmDevelopingRecipe(transferredIngredient, ingredients, result);
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
    }
}
