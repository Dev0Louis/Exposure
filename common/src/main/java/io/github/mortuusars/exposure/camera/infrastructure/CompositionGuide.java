package io.github.mortuusars.exposure.camera.infrastructure;

import io.github.mortuusars.exposure.Exposure;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

@SuppressWarnings("ClassCanBeRecord")
public class CompositionGuide {
    private final String id;

    public CompositionGuide(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Text translate() {
        return Text.translatable("gui." + Exposure.ID + ".composition_guide." + id);
    }

    public void toBuffer(PacketByteBuf buffer) {
        buffer.writeString(id);
    }

    public static CompositionGuide fromBuffer(PacketByteBuf buffer) {
        return CompositionGuides.byIdOrNone(buffer.readString());
    }
}
