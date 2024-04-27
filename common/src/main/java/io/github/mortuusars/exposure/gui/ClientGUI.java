package io.github.mortuusars.exposure.gui;

import io.github.mortuusars.exposure.gui.screen.PhotographScreen;
import io.github.mortuusars.exposure.gui.screen.camera.ViewfinderControlsScreen;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.recipe.FilmDevelopingRecipe;
import io.github.mortuusars.exposure.recipe.PhotographCopyingRecipe;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.recipe.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class ClientGUI {
    public static void openPhotographScreen(List<ItemAndStack<PhotographItem>> photographs) {
        MinecraftClient.getInstance().setScreen(new PhotographScreen(photographs));
    }

    public static void openViewfinderControlsScreen() {
        MinecraftClient.getInstance().setScreen(new ViewfinderControlsScreen());
    }

    public static void addFilmRollDevelopingTooltip(ItemStack filmStack, @Nullable World level, @NotNull List<Text> tooltipComponents, @NotNull TooltipContext isAdvanced) {
        addRecipeTooltip(filmStack, level, tooltipComponents, isAdvanced,
                r -> r instanceof FilmDevelopingRecipe filmDevelopingRecipe
                    && filmDevelopingRecipe.getTransferIngredient().test(filmStack), "item.exposure.film_roll.tooltip.details.develop");
    }

    public static void addPhotographCopyingTooltip(ItemStack photographStack, @Nullable World level, @NotNull List<Text> tooltipComponents, @NotNull TooltipContext isAdvanced) {
        addRecipeTooltip(photographStack, level, tooltipComponents, isAdvanced,
                r -> r instanceof PhotographCopyingRecipe photographCopyingRecipe
                        && photographCopyingRecipe.getTransferIngredient().test(photographStack), "item.exposure.photograph.tooltip.details.copy");
    }

    private static void addRecipeTooltip(ItemStack stack, @Nullable World level,
                                         @NotNull List<Text> tooltipComponents, @NotNull TooltipContext isAdvanced,
                                         Predicate<CraftingRecipe> recipeFilter, String detailsKey) {
        if (level == null)
            return;

        tooltipComponents.add(Text.translatable("tooltip.exposure.hold_for_details"));
        if (!Screen.hasShiftDown()) {
            return;
        }

        Optional<DefaultedList<Ingredient>> recipeIngredients = level.getRecipeManager().listAllOfType(RecipeType.CRAFTING)
                .stream()
                .map(RecipeEntry::value)
                .filter(recipeFilter)
                .findFirst()
                .map(Recipe::getIngredients);

        if (recipeIngredients.isEmpty() || recipeIngredients.get().isEmpty())
            return;

        DefaultedList<Ingredient> ingredients = recipeIngredients.get();

        tooltipComponents.add(Text.empty());

        Style orange = Style.EMPTY.withColor(0xc7954b);
        Style yellow = Style.EMPTY.withColor(0xeeda78);

        tooltipComponents.add(Text.translatable(detailsKey).fillStyle(orange));

        for (int i = 0; i < ingredients.size(); i++) {
            ItemStack[] stacks = ingredients.get(i).getMatchingStacks();

            if (stacks.length == 0)
                tooltipComponents.add(Text.literal("  ").append(Text.literal("?").fillStyle(yellow)));
            else if (stacks.length == 1)
                tooltipComponents.add(Text.literal("  ").append(stacks[0].getName().copy().fillStyle(yellow)));
            else { // Cycle stacks if it's not one:
                int val = (int)Math.ceil((level.getTime() + 10 * i) % (20f * stacks.length) / 20f);
                int index = MathHelper.clamp(val - 1, 0, stacks.length - 1);

                tooltipComponents.add(Text.literal("  ").append(stacks[index].getName().copy().fillStyle(yellow)));
            }
        }
    }
}
