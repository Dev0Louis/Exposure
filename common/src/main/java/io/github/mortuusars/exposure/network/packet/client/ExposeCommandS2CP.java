package io.github.mortuusars.exposure.network.packet.client;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.handler.ClientPacketsHandler;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record ExposeCommandS2CP(int size) implements IPacket {
    public static final Identifier ID = Exposure.resource("expose_command");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeInt(size);
        return buffer;
    }

    public static ExposeCommandS2CP fromBuffer(PacketByteBuf buffer) {
        return new ExposeCommandS2CP(buffer.readInt());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        ClientPacketsHandler.exposeScreenshot(size);
        return true;
    }
}
