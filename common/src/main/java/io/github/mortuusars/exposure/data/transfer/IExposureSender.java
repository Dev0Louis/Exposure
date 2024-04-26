package io.github.mortuusars.exposure.data.transfer;

import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import net.minecraft.entity.player.PlayerEntity;

public interface IExposureSender {
    void send(String id, ExposureSavedData exposureData);
    void sendTo(PlayerEntity player, String id, ExposureSavedData exposureData);
}
