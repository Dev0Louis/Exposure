package io.github.mortuusars.exposure.network.packet.server;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.item.AlbumPage;
import io.github.mortuusars.exposure.menu.AlbumMenu;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public record AlbumSyncNoteC2SP(int pageIndex, String text) implements IPacket {
    public static final Identifier ID = Exposure.id("album_update_note");

    @Override
    public Identifier getId() {
        return ID;
    }

    public static AlbumSyncNoteC2SP fromBuffer(PacketByteBuf buffer) {
        return new AlbumSyncNoteC2SP(buffer.readInt(), buffer.readString());
    }

    @Override
    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeInt(pageIndex);
        buffer.writeString(text);
        return buffer;
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        Preconditions.checkState(player != null, "Cannot handle packet: Player was null");

        if (!(player.currentScreenHandler instanceof AlbumMenu albumMenu))
            throw new IllegalStateException("Player receiving this packet should have AlbumMenu open. Current menu: " + player.currentScreenHandler);

        AlbumPage page = albumMenu.getPages().get(pageIndex);
        page.setNote(Either.left(text));
        albumMenu.updateAlbumStack();

        return true;
    }
}
