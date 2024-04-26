package io.github.mortuusars.exposure.item;

import io.github.mortuusars.exposure.Config;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.StringHelper;
import net.minecraft.world.World;

public class SignedAlbumItem extends AlbumItem {
    public SignedAlbumItem(Settings properties) {
        super(properties);
    }

    @Override
    public boolean isEditable() {
        return false;
    }

    @Override
    public @NotNull Text getName(ItemStack stack) {
        if (stack.getNbt() != null && !StringHelper.isEmpty(stack.getNbt().getString(TAG_TITLE))) {
            return Text.literal(stack.getNbt().getString(TAG_TITLE));
        }
        return super.getName(stack);
    }
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World level, List<Text> tooltipComponents, TooltipContext isAdvanced) {
        if (stack.getNbt() != null) {
            NbtCompound compoundTag = stack.getNbt();
            String author = compoundTag.getString(TAG_AUTHOR);
            if (!StringHelper.isEmpty(author)) {
                tooltipComponents.add(Text.translatable("gui.exposure.album.by_author", author).formatted(Formatting.GRAY));
            }
        }
        super.appendTooltip(stack, level, tooltipComponents, isAdvanced);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return Config.Client.SIGNED_ALBUM_GLINT.get();
    }
}
