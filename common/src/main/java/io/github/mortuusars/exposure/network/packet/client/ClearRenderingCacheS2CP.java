package io.github.mortuusars.exposure.network.packet.client;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.handler.ClientPacketsHandler;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class ClearRenderingCacheS2CP implements IPacket {
    public static final Identifier ID = Exposure.id("clear_rendering_cache");

    @Override
    public Identifier getId() {
        return ID;
    }

    public static ClearRenderingCacheS2CP fromBuffer(PacketByteBuf buffer) {
        return new ClearRenderingCacheS2CP();
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        ClientPacketsHandler.clearRenderingCache();
        return true;
    }
}