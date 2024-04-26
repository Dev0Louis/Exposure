package io.github.mortuusars.exposure.network;


import com.google.common.base.Preconditions;
import dev.architectury.injectables.annotations.ExpectPlatform;
import io.github.mortuusars.exposure.network.packet.IPacket;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

public class Packets {
    @ExpectPlatform
    public static void sendToServer(IPacket packet) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendToClient(IPacket packet, ServerPlayerEntity player) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void sendToAllClients(IPacket packet) {
        throw new AssertionError();
    }

    public static void sendToClients(IPacket packet, PlayerManager playerList, @Nullable ServerPlayerEntity excludedPlayer) {
        for (ServerPlayerEntity player : playerList.getPlayerList()) {
            if (player != excludedPlayer)
                sendToClient(packet, player);
        }
    }

    public static void sendToClients(IPacket packet, ServerPlayerEntity origin, Predicate<ServerPlayerEntity> filter) {
        Preconditions.checkState(origin.getServer() != null, "Server cannot be null");
        for (ServerPlayerEntity player : origin.getServer().getPlayerManager().getPlayerList()) {
            if (filter.test(player))
                sendToClient(packet, player);
        }
    }

    public static void sendToOtherClients(IPacket packet, ServerPlayerEntity excludedPlayer) {
        sendToClients(packet, excludedPlayer, serverPlayer -> !serverPlayer.equals(excludedPlayer));
    }

    public static void sendToOtherClients(IPacket packet, ServerPlayerEntity excludedPlayer, Predicate<ServerPlayerEntity> filter) {
        sendToClients(packet, excludedPlayer, serverPlayer -> !serverPlayer.equals(excludedPlayer) && filter.test(serverPlayer));
    }
}