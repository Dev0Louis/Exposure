package io.github.mortuusars.exposure.integration.jei.category;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.integration.jei.ExposureJeiPlugin;
import io.github.mortuusars.exposure.integration.jei.recipe.PhotographPrintingJeiRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PhotographPrintingCategory implements IRecipeCategory<PhotographPrintingJeiRecipe> {
    private static final Identifier TEXTURE = Exposure.resource("textures/gui/jei/photograph_printing.png");
    private final Text title;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableStatic filmDrawable;

    public PhotographPrintingCategory(IGuiHelper guiHelper) {
        title = Text.translatable("jei.exposure.photograph_printing.title");
        background = guiHelper.createDrawable(TEXTURE, 0, 0, 170, 80);
        icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Exposure.Items.LIGHTROOM.get()));

        filmDrawable = guiHelper.createDrawable(TEXTURE, 0, 80, 170, 80);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, @NotNull PhotographPrintingJeiRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 78, 17)
                .addItemStack(new ItemStack(recipe.getFilmType() == FilmType.COLOR ?
                        Exposure.Items.DEVELOPED_COLOR_FILM.get()
                        : Exposure.Items.DEVELOPED_BLACK_AND_WHITE_FILM.get()))
                .setSlotName("Film");


        List<ItemStack> papers = Registries.ITEM.getEntryList(Exposure.Tags.Items.PHOTO_PAPERS)
                .map(holders -> holders.stream()
                        .map(itemHolder -> new ItemStack(itemHolder.value())).collect(Collectors.toList()))
                .orElse(Collections.emptyList());

        builder.addSlot(RecipeIngredientRole.CATALYST, 6, 55)
                .addItemStacks(papers)
                .setSlotName("Paper");

        if (recipe.getFilmType() == FilmType.COLOR) {
            List<ItemStack> cyanDyes = Registries.ITEM.getEntryList(Exposure.Tags.Items.CYAN_PRINTING_DYES)
                    .map(holders -> holders.stream()
                            .map(itemHolder -> new ItemStack(itemHolder.value())).collect(Collectors.toList()))
                    .orElse(Collections.emptyList());

            builder.addSlot(RecipeIngredientRole.CATALYST, 40, 55)
                    .addItemStacks(cyanDyes)
                    .setSlotName("Cyan");

            List<ItemStack> magentaDyes = Registries.ITEM.getEntryList(Exposure.Tags.Items.MAGENTA_PRINTING_DYES)
                    .map(holders -> holders.stream()
                            .map(itemHolder -> new ItemStack(itemHolder.value())).collect(Collectors.toList()))
                    .orElse(Collections.emptyList());

            builder.addSlot(RecipeIngredientRole.CATALYST, 58, 55)
                    .addItemStacks(magentaDyes)
                    .setSlotName("Magenta");

            List<ItemStack> yellowDyes = Registries.ITEM.getEntryList(Exposure.Tags.Items.YELLOW_PRINTING_DYES)
                    .map(holders -> holders.stream()
                            .map(itemHolder -> new ItemStack(itemHolder.value())).collect(Collectors.toList()))
                    .orElse(Collections.emptyList());

            builder.addSlot(RecipeIngredientRole.CATALYST, 76, 55)
                    .addItemStacks(yellowDyes)
                    .setSlotName("Yellow");
        }

        List<ItemStack> blackDyes = Registries.ITEM.getEntryList(Exposure.Tags.Items.BLACK_PRINTING_DYES)
                .map(holders -> holders.stream()
                        .map(itemHolder -> new ItemStack(itemHolder.value())).collect(Collectors.toList()))
                .orElse(Collections.emptyList());

        builder.addSlot(RecipeIngredientRole.CATALYST, 94, 55)
                .addItemStacks(blackDyes)
                .setSlotName("Black");

        builder.addSlot(RecipeIngredientRole.OUTPUT, 144, 55)
                .addItemStack(new ItemStack(Exposure.Items.PHOTOGRAPH.get()));
    }

    @Override
    public void draw(PhotographPrintingJeiRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView, @NotNull DrawContext guiGraphics, double mouseX, double mouseY) {
        if (recipe.getFilmType() == FilmType.COLOR)
            RenderSystem.setShaderColor(1.1F, 0.86F, 0.66F, 1.0F);
        filmDrawable.draw(guiGraphics);
    }

    @Override
    public @NotNull RecipeType<PhotographPrintingJeiRecipe> getRecipeType() {
        return ExposureJeiPlugin.PHOTOGRAPH_PRINTING_RECIPE_TYPE;
    }

    @Override
    public @NotNull Text getTitle() {
        return title;
    }

    @Override
    public @NotNull IDrawable getBackground() {
        return background;
    }

    @Override
    public @NotNull IDrawable getIcon() {
        return icon;
    }
}