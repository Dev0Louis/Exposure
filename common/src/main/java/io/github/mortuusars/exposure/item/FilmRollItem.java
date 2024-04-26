package io.github.mortuusars.exposure.item;

import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.gui.ClientGUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class FilmRollItem extends Item implements IFilmItem {
    public static final String FRAME_SIZE_TAG = "FrameSize";

    private final FilmType filmType;
    private final int defaultFrameSize;
    private final int barColor;

    public FilmRollItem(FilmType filmType, int defaultFrameSize, int barColor, Settings properties) {
        super(properties);
        this.filmType = filmType;
        this.defaultFrameSize = defaultFrameSize;
        this.barColor = barColor;
    }

    @Override
    public FilmType getType() {
        return filmType;
    }

    public int getFrameSize(ItemStack filmStack) {
        if (filmStack.getNbt() != null && filmStack.getOrCreateNbt().contains(FRAME_SIZE_TAG, NbtElement.INT_TYPE))
            return MathHelper.clamp(filmStack.getOrCreateNbt().getInt(FRAME_SIZE_TAG), 1, 2048);
        else
            return defaultFrameSize;
    }

    public boolean isItemBarVisible(@NotNull ItemStack stack) {
        return getExposedFramesCount(stack) > 0;
    }

    public int getItemBarStep(@NotNull ItemStack stack) {
        return Math.min(1 + 12 * getExposedFramesCount(stack) / getMaxFrameCount(stack), 13);
    }

    public int getItemBarColor(@NotNull ItemStack stack) {
        return barColor;
    }

    public void addFrame(ItemStack filmStack, NbtCompound frame) {
        NbtCompound tag = filmStack.getOrCreateNbt();

        if (!tag.contains("Frames", NbtElement.LIST_TYPE)) {
            tag.put("Frames", new NbtList());
        }

        NbtList listTag = tag.getList("Frames", NbtElement.COMPOUND_TYPE);

        if (listTag.size() >= getMaxFrameCount(filmStack))
            throw new IllegalStateException("Cannot add more frames than film could fit. Size: " + listTag.size());

        listTag.add(frame);
        tag.put("Frames", listTag);
    }

    public boolean canAddFrame(ItemStack filmStack) {
        if (!filmStack.hasNbt() || !filmStack.getOrCreateNbt().contains("Frames", NbtElement.LIST_TYPE))
            return true;

        return filmStack.getOrCreateNbt().getList("Frames", NbtElement.COMPOUND_TYPE).size() < getMaxFrameCount(filmStack);
    }

    @Override
    public void appendTooltip(@NotNull ItemStack stack, @Nullable World level, @NotNull List<Text> tooltipComponents, @NotNull TooltipContext isAdvanced) {
        int exposedFrames = getExposedFramesCount(stack);
        if (exposedFrames > 0) {
            int totalFrames = getMaxFrameCount(stack);
            tooltipComponents.add(Text.translatable("item.exposure.film_roll.tooltip.frame_count", exposedFrames, totalFrames)
                    .formatted(Formatting.GRAY));
        }

        int frameSize = getFrameSize(stack);
        if (frameSize != defaultFrameSize) {
            tooltipComponents.add(Text.translatable("item.exposure.film_roll.tooltip.frame_size",
                    Text.literal(String.format("%.1f", frameSize / 10f)))
                            .formatted(Formatting.GRAY));
        }


        // Create compat:
        int developingStep = stack.getNbt() != null ? stack.getNbt().getInt("CurrentDevelopingStep") : 0;
        if (Config.Common.CREATE_SPOUT_DEVELOPING_ENABLED.get() && developingStep > 0) {
            List<? extends String> totalSteps = Config.Common.spoutDevelopingSequence(getType()).get();

            MutableText stepsComponent = Text.literal("");

            for (int i = 0; i < developingStep; i++) {
                stepsComponent.append(Text.literal("I").formatted(Formatting.GOLD));
            }

            for (int i = developingStep; i < totalSteps.size(); i++) {
                stepsComponent.append(Text.literal("I").formatted(Formatting.DARK_GRAY));
            }

            tooltipComponents.add(Text.translatable("item.exposure.film_roll.tooltip.developing_step", stepsComponent)
                    .formatted(Formatting.GOLD));
        }

        if (exposedFrames > 0 && !PlatformHelper.isModLoaded("jei") && Config.Client.RECIPE_TOOLTIPS_WITHOUT_JEI.get()) {
            ClientGUI.addFilmRollDevelopingTooltip(stack, level, tooltipComponents, isAdvanced);
        }
    }
}
