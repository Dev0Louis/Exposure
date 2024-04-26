package io.github.mortuusars.exposure.camera.infrastructure;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import java.util.Objects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;

public class ShutterSpeed {
    public static final ShutterSpeed DEFAULT = new ShutterSpeed("60");

    private final String text;
    private final float valueMilliseconds;

    public ShutterSpeed(String shutterSpeed) {
        this.text = shutterSpeed;
        this.valueMilliseconds = parseMilliseconds(shutterSpeed);
        Preconditions.checkState(valueMilliseconds != -1,  shutterSpeed + " is not valid. (format should be: '1/60', '60', '2\"')");
        Preconditions.checkState(valueMilliseconds > 0, "Shutter Speed cannot be 0 or smaller.");
    }

    private float parseMilliseconds(String shutterSpeed) {
        shutterSpeed = shutterSpeed.trim();

        if (shutterSpeed.contains("\""))
            return Integer.parseInt(shutterSpeed.replace("\"", "")) * 1000;
        else if (shutterSpeed.contains("1/"))
            return 1f / Integer.parseInt(shutterSpeed.replace("1/", "")) * 1000;
        else
            return 1f / Integer.parseInt(shutterSpeed) * 1000;
    }

    public String getFormattedText() {
        if (getMilliseconds() < 999 && !text.startsWith("1/"))
            return "1/" + text;

        return text;
    }

    public float getMilliseconds() {
        return valueMilliseconds;
    }

    public int getTicks() {
        return Math.max(1, (int)(valueMilliseconds * 20 / 1000f));
    }

    public float getStopsDifference(ShutterSpeed relative) {
        return (float) (Math.log(valueMilliseconds / relative.getMilliseconds()) / Math.log(2));
    }

    public NbtCompound save(NbtCompound tag) {
        tag.putString("ShutterSpeed", text);
        return tag;
    }

    public static ShutterSpeed loadOrDefault(NbtCompound tag) {
        try {
            if (tag.contains("ShutterSpeed", NbtElement.STRING_TYPE)) {
                String shutterSpeed = tag.getString("ShutterSpeed");
                return new ShutterSpeed(shutterSpeed);
            }
        }
        catch (IllegalStateException e) {
            LogUtils.getLogger().error("Cannot load a shutter speed from tag: " + e);
        }

        return DEFAULT;
    }
    
    public void toBuffer(PacketByteBuf buffer) {
        buffer.writeString(text);
    }

    public static ShutterSpeed fromBuffer(PacketByteBuf buffer) {
        return new ShutterSpeed(buffer.readString());
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShutterSpeed that = (ShutterSpeed) o;
        return Float.compare(that.valueMilliseconds, valueMilliseconds) == 0 && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, valueMilliseconds);
    }
}
