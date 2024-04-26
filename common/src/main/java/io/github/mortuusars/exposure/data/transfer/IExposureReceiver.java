package io.github.mortuusars.exposure.data.transfer;

import net.minecraft.nbt.NbtCompound;

public interface IExposureReceiver {
    void receivePart(String id, int width, int height, NbtCompound properties, int offset, byte[] partBytes);
}
