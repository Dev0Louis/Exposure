package io.github.mortuusars.exposure.network.packet;

import io.github.mortuusars.exposure.network.PacketDirection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public interface IPacket {
    default PacketByteBuf toBuffer(PacketByteBuf buffer) { return buffer; }
    /**
     * @param player will be null when on the client.
     */
    boolean handle(PacketDirection direction, @Nullable PlayerEntity player);
    Identifier getId();
}