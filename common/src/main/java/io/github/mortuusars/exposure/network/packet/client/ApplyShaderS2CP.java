package io.github.mortuusars.exposure.network.packet.client;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.handler.ClientPacketsHandler;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record ApplyShaderS2CP(Identifier shaderLocation) implements IPacket {
    public static final Identifier ID = Exposure.resource("apply_shader");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeIdentifier(shaderLocation);
        return buffer;
    }

    public static ApplyShaderS2CP fromBuffer(PacketByteBuf buffer) {
        return new ApplyShaderS2CP(buffer.readIdentifier());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        ClientPacketsHandler.applyShader(this);
        return true;
    }
}
