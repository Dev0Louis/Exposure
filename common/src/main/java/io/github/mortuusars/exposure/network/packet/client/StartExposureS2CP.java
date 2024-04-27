package io.github.mortuusars.exposure.network.packet.client;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.handler.ClientPacketsHandler;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record StartExposureS2CP(@NotNull String exposureId, @NotNull Hand activeHand,
                                boolean flashHasFired, int lightLevel) implements IPacket {
    public static final Identifier ID = Exposure.id("start_exposure");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        Preconditions.checkState(exposureId.length() > 0, "path cannot be empty.");
        buffer.writeString(exposureId);
        buffer.writeEnumConstant(activeHand);
        buffer.writeBoolean(flashHasFired);
        buffer.writeInt(lightLevel);
        return buffer;
    }

    public static StartExposureS2CP fromBuffer(PacketByteBuf buffer) {
        return new StartExposureS2CP(buffer.readString(), buffer.readEnumConstant(Hand.class), buffer.readBoolean(), buffer.readInt());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        ClientPacketsHandler.startExposure(this);
        return true;
    }
}
