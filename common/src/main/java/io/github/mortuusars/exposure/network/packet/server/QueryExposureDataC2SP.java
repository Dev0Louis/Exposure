package io.github.mortuusars.exposure.network.packet.server;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureServer;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record QueryExposureDataC2SP(String id) implements IPacket {
    public static final Identifier ID = Exposure.id("query_exposure_data");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeString(id);
        return buffer;
    }

    public static QueryExposureDataC2SP fromBuffer(PacketByteBuf buffer) {
        return new QueryExposureDataC2SP(buffer.readString());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        Preconditions.checkArgument(player != null, "Cannot handle QueryExposureDataPacket: Player was null");

        Optional<ExposureSavedData> exposureSavedData = ExposureServer.getExposureStorage().getOrQuery(id);

        if (exposureSavedData.isEmpty())
            LogUtils.getLogger().error("Cannot get exposure data with an id '" + id + "'. Result is null.");
        else {
            ExposureServer.getExposureSender().sendTo(player, id, exposureSavedData.get());
        }

        return true;
    }
}
