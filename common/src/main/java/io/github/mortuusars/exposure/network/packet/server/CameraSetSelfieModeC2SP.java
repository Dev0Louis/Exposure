package io.github.mortuusars.exposure.network.packet.server;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record CameraSetSelfieModeC2SP(Hand hand, boolean isInSelfieMode, boolean effects) implements IPacket {
    public static final Identifier ID = Exposure.id("camera_set_selfie_mode");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeEnumConstant(hand);
        buffer.writeBoolean(isInSelfieMode);
        buffer.writeBoolean(effects);
        return buffer;
    }

    public static CameraSetSelfieModeC2SP fromBuffer(PacketByteBuf buffer) {
        return new CameraSetSelfieModeC2SP(buffer.readEnumConstant(Hand.class), buffer.readBoolean(), buffer.readBoolean());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        Preconditions.checkState(player != null, "Cannot handle packet: Player was null");

        ItemStack itemInHand = player.getStackInHand(hand);
        if (!(itemInHand.getItem() instanceof CameraItem cameraItem))
            throw new IllegalStateException("Item in hand in not a Camera.");

        if (effects)
            cameraItem.setSelfieModeWithEffects(player, itemInHand, isInSelfieMode);
        else
            cameraItem.setSelfieMode(itemInHand, isInSelfieMode);

        return true;
    }
}