package io.github.mortuusars.exposure.sound;

import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.client.PlayOnePerPlayerSoundS2CP;
import io.github.mortuusars.exposure.network.packet.client.StopOnePerPlayerSoundS2CP;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

public class OnePerPlayerSounds {
    public static void play(PlayerEntity sourcePlayer, SoundEvent soundEvent, SoundCategory source, float volume, float pitch) {
        if (sourcePlayer.getWorld().isClient) {
            OnePerPlayerSoundsClient.play(sourcePlayer, soundEvent, source, volume, pitch);
        }
        else if (sourcePlayer instanceof ServerPlayerEntity serverSourcePlayer) {
            Packets.sendToOtherClients(new PlayOnePerPlayerSoundS2CP(serverSourcePlayer.getUuid(),
                            soundEvent,source, volume, pitch),
                    serverSourcePlayer,
                    serverPlayer -> serverSourcePlayer.distanceTo(serverPlayer) < soundEvent.getDistanceToTravel(1f));
        }
    }

    public static void stop(PlayerEntity sourcePlayer, SoundEvent soundEvent) {
        if (sourcePlayer.getWorld().isClient) {
            OnePerPlayerSoundsClient.stop(sourcePlayer, soundEvent);
        }
        else if (sourcePlayer instanceof ServerPlayerEntity serverSourcePlayer) {
            Packets.sendToOtherClients(new StopOnePerPlayerSoundS2CP(serverSourcePlayer.getUuid(), soundEvent),
                    serverSourcePlayer, serverPlayer -> serverSourcePlayer.distanceTo(serverPlayer) < soundEvent.getDistanceToTravel(1f));
        }
    }
}
