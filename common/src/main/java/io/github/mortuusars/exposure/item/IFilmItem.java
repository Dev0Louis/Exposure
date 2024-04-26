package io.github.mortuusars.exposure.item;

import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

public interface IFilmItem {
    FilmType getType();
    default int getDefaultMaxFrameCount(ItemStack filmStack) {
        return 16;
    }

    default int getMaxFrameCount(ItemStack filmStack) {
        if (filmStack.getNbt() != null && filmStack.getOrCreateNbt().contains("FrameCount", NbtElement.INT_TYPE))
            return filmStack.getOrCreateNbt().getInt("FrameCount");
        else
            return getDefaultMaxFrameCount(filmStack);
    }

    default boolean hasExposedFrame(ItemStack filmStack, int index) {
        if (index < 0 || filmStack.getNbt() == null || !filmStack.getNbt().contains("Frames", NbtElement.LIST_TYPE))
            return false;

        NbtList list = filmStack.getNbt().getList("Frames", NbtElement.COMPOUND_TYPE);
        return index < list.size();
    }

    default int getExposedFramesCount(ItemStack stack) {
        return stack.hasNbt() && stack.getOrCreateNbt().contains("Frames", NbtElement.LIST_TYPE) ?
                stack.getOrCreateNbt().getList("Frames", NbtElement.COMPOUND_TYPE).size() : 0;
    }

    default NbtList getExposedFrames(ItemStack filmStack) {
        return filmStack.getNbt() != null ? filmStack.getNbt().getList("Frames", NbtElement.COMPOUND_TYPE) : new NbtList();
    }
}
