package io.github.mortuusars.exposure.network;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

public class NetworkContext {
    @Nullable
    private final PlayerEntity player;
    private final PacketDirection receivingSide;

    public NetworkContext(@Nullable PlayerEntity player, PacketDirection receivingSide) {
        this.player = player;
        this.receivingSide = receivingSide;
    }

    public @Nullable PlayerEntity getPlayer() {
        return player;
    }
}
