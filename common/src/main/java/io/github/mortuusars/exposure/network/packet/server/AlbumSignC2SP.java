package io.github.mortuusars.exposure.network.packet.server;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.item.AlbumItem;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record AlbumSignC2SP(String title) implements IPacket {
    public static final Identifier ID = Exposure.resource("album_sign");

    @Override
    public Identifier getId() {
        return ID;
    }

    public static AlbumSignC2SP fromBuffer(PacketByteBuf buffer) {
        return new AlbumSignC2SP(buffer.readString());
    }

    @Override
    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeString(title);
        return buffer;
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        Preconditions.checkState(player != null, "Cannot handle packet: Player was null");

        Hand hand;
        if (player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof AlbumItem albumItem && albumItem.isEditable())
            hand = Hand.MAIN_HAND;
        else if (player.getStackInHand(Hand.OFF_HAND).getItem() instanceof AlbumItem albumItem && albumItem.isEditable())
            hand = Hand.OFF_HAND;
        else
            throw new IllegalStateException("Player receiving this packet should have an album in one of the hands.");

        ItemStack albumStack = player.getStackInHand(hand);
        AlbumItem albumItem = (AlbumItem) albumStack.getItem();

        ItemStack signedAlbum = albumItem.sign(albumStack, title, player.getName().getString());
        player.setStackInHand(hand, signedAlbum);

        player.getWorld().playSoundFromEntity(null, player, SoundEvents.ENTITY_VILLAGER_WORK_CARTOGRAPHER, SoundCategory.PLAYERS, 0.8f ,1f);

        return true;
    }
}
