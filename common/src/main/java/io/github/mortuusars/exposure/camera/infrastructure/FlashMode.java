package io.github.mortuusars.exposure.camera.infrastructure;

import io.github.mortuusars.exposure.Exposure;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.NotNull;

public enum FlashMode implements StringIdentifiable {
    OFF("off"),
    ON("on"),
    AUTO("auto");

    private final String id;

    FlashMode(String id) {
        this.id = id;
    }

    public static FlashMode byIdOrOff(String id) {
        for (FlashMode guide : values()) {
            if (guide.id.equals(id))
                return guide;
        }

        return OFF;
    }

    public String getId() {
        return id;
    }

    @Override
    public @NotNull String asString() {
        return id;
    }

    public Text translate() {
        return Text.translatable("gui." + Exposure.ID + ".flash_mode." + id);
    }
    public void toBuffer(PacketByteBuf buffer) {
        buffer.writeString(getId());
    }

    public static FlashMode fromBuffer(PacketByteBuf buffer) {
        return FlashMode.byIdOrOff(buffer.readString());
    }
}
