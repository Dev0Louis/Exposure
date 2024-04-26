package io.github.mortuusars.exposure.network.packet.client;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.handler.ClientPacketsHandler;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record ExposureChangedS2CP(String exposureId) implements IPacket {
    public static final Identifier ID = Exposure.resource("exposure_changed");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeString(exposureId);
        return buffer;
    }

    public static ExposureChangedS2CP fromBuffer(PacketByteBuf buffer) {
        return new ExposureChangedS2CP(buffer.readString());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        ClientPacketsHandler.onExposureChanged(this);
        return true;
    }
}
