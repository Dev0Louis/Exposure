package io.github.mortuusars.exposure.network.packet.server;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record CameraSetZoomC2SP(double focalLength) implements IPacket {
    public static final Identifier ID = Exposure.id("camera_set_zoom");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeDouble(focalLength);
        return buffer;
    }

    public static CameraSetZoomC2SP fromBuffer(PacketByteBuf buffer) {
        return new CameraSetZoomC2SP(buffer.readDouble());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        Preconditions.checkState(player != null, "Cannot handle packet: Player was null");

        CameraInHand camera = CameraInHand.getActive(player);
        if (!camera.isEmpty()) {
            camera.getItem().setZoom(camera.getStack(), focalLength);
        }

        return true;
    }
}
