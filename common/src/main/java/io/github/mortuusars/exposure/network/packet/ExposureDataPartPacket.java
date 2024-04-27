package io.github.mortuusars.exposure.network.packet;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record ExposureDataPartPacket(String id, int width, int height, NbtCompound properties, int offset, byte[] partBytes) implements IPacket {
    public static final Identifier ID = Exposure.id("exposure_data_part");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeString(id);
        buffer.writeInt(width);
        buffer.writeInt(height);
        buffer.writeNbt(properties);
        buffer.writeInt(offset);
        buffer.writeByteArray(partBytes);
        return buffer;
    }

    public static ExposureDataPartPacket fromBuffer(PacketByteBuf buffer) {
        return new ExposureDataPartPacket(buffer.readString(), buffer.readInt(), buffer.readInt(),
                (NbtCompound) buffer.readNbt(NbtSizeTracker.ofUnlimitedBytes()), buffer.readInt(), buffer.readByteArray());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        direction.getExposureReceiver().receivePart(id, width, height, properties, offset, partBytes);
        return true;
    }
}
