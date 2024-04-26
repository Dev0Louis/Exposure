package io.github.mortuusars.exposure.item;

import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

public class DevelopedFilmItem extends Item implements IFilmItem {
    private final FilmType type;

    public DevelopedFilmItem(FilmType type, Settings properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public FilmType getType() {
        return type;
    }

    @Override
    public void appendTooltip(@NotNull ItemStack stack, @Nullable World level, @NotNull List<Text> tooltipComponents, @NotNull TooltipContext isAdvanced) {
        int exposedFrames = getExposedFramesCount(stack);
        if (exposedFrames > 0) {
            tooltipComponents.add(Text.translatable("item.exposure.developed_film.tooltip.frame_count", exposedFrames)
                    .formatted(Formatting.GRAY));
        }
    }
}
