package io.github.mortuusars.exposure.network.packet.client;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.handler.ClientPacketsHandler;
import io.github.mortuusars.exposure.network.packet.IPacket;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public final class ShowExposureS2CP implements IPacket {
    public static final Identifier ID = Exposure.id("show_exposure");

    private final String idOrPath;
    private final boolean isTexture;
    private final boolean negative;
    private final boolean latest;

    public ShowExposureS2CP(String path, boolean isTexture, boolean negative, boolean latest) {
        this.idOrPath = path;
        this.isTexture = isTexture;
        this.negative = negative;
        this.latest = latest;
    }

    public static ShowExposureS2CP latest(boolean negative) {
        return new ShowExposureS2CP("", false, negative, true);
    }

    public static ShowExposureS2CP id(String id, boolean negative) {
        return new ShowExposureS2CP(id, false, negative, false);
    }

    public static ShowExposureS2CP texture(String path, boolean negative) {
        return new ShowExposureS2CP(path, true, negative, false);
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeString(idOrPath);
        buffer.writeBoolean(isTexture);
        buffer.writeBoolean(negative);
        buffer.writeBoolean(latest);
        return buffer;
    }

    public static ShowExposureS2CP fromBuffer(PacketByteBuf buffer) {
        return new ShowExposureS2CP(buffer.readString(), buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        ClientPacketsHandler.showExposure(this);
        return true;
    }

    public String idOrPath() {
        return idOrPath;
    }

    public boolean isTexture() {
        return isTexture;
    }

    public boolean negative() {
        return negative;
    }

    public boolean latest() {
        return latest;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ShowExposureS2CP) obj;
        return Objects.equals(this.idOrPath, that.idOrPath) &&
                this.isTexture == that.isTexture &&
                this.negative == that.negative &&
                this.latest == that.latest;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idOrPath, isTexture, negative, latest);
    }

    @Override
    public String toString() {
        return "ShowExposureS2CP[" +
                "path=" + idOrPath + ", " +
                "isTexture=" + isTexture + ", " +
                "negative=" + negative + ", " +
                "latest=" + latest + ']';
    }

}
