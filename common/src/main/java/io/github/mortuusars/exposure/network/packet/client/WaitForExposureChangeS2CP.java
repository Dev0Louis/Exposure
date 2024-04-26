package io.github.mortuusars.exposure.network.packet.client;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.handler.ClientPacketsHandler;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record WaitForExposureChangeS2CP(String exposureId) implements IPacket {
    public static final Identifier ID = Exposure.resource("wait_for_exposure_change");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeString(exposureId);
        return buffer;
    }

    public static WaitForExposureChangeS2CP fromBuffer(PacketByteBuf buffer) {
        return new WaitForExposureChangeS2CP(buffer.readString());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        ClientPacketsHandler.waitForExposureChange(this);
        return true;
    }
}
