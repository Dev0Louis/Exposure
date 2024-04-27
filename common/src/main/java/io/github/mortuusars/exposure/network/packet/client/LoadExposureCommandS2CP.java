package io.github.mortuusars.exposure.network.packet.client;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.handler.ClientPacketsHandler;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record LoadExposureCommandS2CP(String id, String path, int size, boolean dither) implements IPacket {
    public static final Identifier ID = Exposure.id("load_exposure");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeString(id);
        buffer.writeString(path);
        buffer.writeInt(size);
        buffer.writeBoolean(dither);
        return buffer;
    }

    public static LoadExposureCommandS2CP fromBuffer(PacketByteBuf buffer) {
        return new LoadExposureCommandS2CP(buffer.readString(), buffer.readString(), buffer.readInt(), buffer.readBoolean());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        ClientPacketsHandler.loadExposure(id, path, size, dither);
        return true;
    }
}
