package io.github.mortuusars.exposure.network.packet.server;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.CompositionGuide;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record CameraSetCompositionGuideC2SP(CompositionGuide guide) implements IPacket {
    public static final Identifier ID = Exposure.resource("camera_set_composition_guide");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        guide.toBuffer(buffer);
        return buffer;
    }

    public static CameraSetCompositionGuideC2SP fromBuffer(PacketByteBuf buffer) {
        return new CameraSetCompositionGuideC2SP(CompositionGuide.fromBuffer(buffer));
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        Preconditions.checkState(player != null, "Cannot handle packet: Player was null");

        CameraInHand camera = CameraInHand.getActive(player);
        if (!camera.isEmpty()) {
            camera.getItem().setCompositionGuide(camera.getStack(), guide);
        }

        return true;
    }
}
