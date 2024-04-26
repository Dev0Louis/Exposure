package io.github.mortuusars.exposure.network.packet.server;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.FlashMode;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record CameraSetFlashModeC2SP(FlashMode flashMode) implements IPacket {
    public static final Identifier ID = Exposure.resource("camera_set_flash_mode");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        flashMode.toBuffer(buffer);
        return buffer;
    }

    public static CameraSetFlashModeC2SP fromBuffer(PacketByteBuf buffer) {
        return new CameraSetFlashModeC2SP(FlashMode.fromBuffer(buffer));
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        Preconditions.checkState(player != null, "Cannot handle packet: Player was null");

        CameraInHand camera = CameraInHand.getActive(player);
        if (!camera.isEmpty()) {
            camera.getItem().setFlashMode(camera.getStack(), flashMode);
        }

        return true;
    }
}
