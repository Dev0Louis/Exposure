package io.github.mortuusars.exposure.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.mortuusars.exposure.Exposure;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.collection.DefaultedList;
import org.jetbrains.annotations.NotNull;

public class PhotographAgingRecipe extends AbstractNbtTransferringRecipe{
    public PhotographAgingRecipe(Ingredient transferIngredient,
                                 DefaultedList<Ingredient> ingredients, ItemStack result) {
        super(transferIngredient, ingredients, result);
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Exposure.RecipeSerializers.PHOTOGRAPH_AGING.get();
    }

    public static class Serializer implements RecipeSerializer<PhotographAgingRecipe> {

        Codec<DefaultedList<Ingredient>> DEFAULTED_INGREDIENT_LIST = RecordCodecBuilder.create(instance -> instance.group(
                Codec.list(Ingredient.DISALLOW_EMPTY_CODEC).fieldOf("ingredients").forGetter(ingredients -> ingredients.delegate),
                Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("defaultIngredient").forGetter(ingredients -> ingredients.initialElement)
        ).apply(instance, DefaultedList::new));

        Codec<PhotographAgingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("transferIngredient").forGetter(PhotographAgingRecipe::getTransferIngredient),
                DEFAULTED_INGREDIENT_LIST.fieldOf("ingredients").forGetter(PhotographAgingRecipe::getIngredients),
                ItemStack.CODEC.fieldOf("result").forGetter(PhotographAgingRecipe::getResult)

        ).apply(
                instance, PhotographAgingRecipe::new
        ));

        @Override
        public Codec<PhotographAgingRecipe> codec() {
            return CODEC;
        }

        @Override
        public PhotographAgingRecipe read(PacketByteBuf buffer) {
            Ingredient transferredIngredient = Ingredient.fromPacket(buffer);
            int ingredientsCount = buffer.readVarInt();
            DefaultedList<Ingredient> ingredients = DefaultedList.ofSize(ingredientsCount, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.fromPacket(buffer));
            ItemStack result = buffer.readItemStack();

            return new PhotographAgingRecipe(transferredIngredient, ingredients, result);
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
    }
}
