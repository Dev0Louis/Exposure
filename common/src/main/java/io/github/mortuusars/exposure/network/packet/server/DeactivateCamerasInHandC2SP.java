package io.github.mortuusars.exposure.network.packet.server;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record DeactivateCamerasInHandC2SP() implements IPacket {
    public static final Identifier ID = Exposure.resource("deactivate_cameras_in_hand");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        return buffer;
    }

    public static DeactivateCamerasInHandC2SP fromBuffer(PacketByteBuf buffer) {
        return new DeactivateCamerasInHandC2SP();
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        if (player == null)
            throw new IllegalStateException("Cannot handle the packet: Player was null");

        CameraInHand.deactivate(player);
        return true;
    }
}
