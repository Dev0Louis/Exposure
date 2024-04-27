package io.github.mortuusars.exposure.network.packet.client;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.network.PacketDirection;
import io.github.mortuusars.exposure.network.packet.IPacket;
import io.github.mortuusars.exposure.sound.OnePerPlayerSounds;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public record StopOnePerPlayerSoundS2CP(UUID sourcePlayerId, SoundEvent soundEvent) implements IPacket {
    public static final Identifier ID = Exposure.id("stop_one_per_player_sound");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeUuid(sourcePlayerId);
        buffer.writeIdentifier(soundEvent.getId());
        return buffer;
    }

    public static StopOnePerPlayerSoundS2CP fromBuffer(PacketByteBuf buffer) {
        UUID uuid = buffer.readUuid();
        Identifier soundEventLocation = buffer.readIdentifier();
        @Nullable SoundEvent soundEvent = Registries.SOUND_EVENT.get(soundEventLocation);
        if (soundEvent == null)
            soundEvent = SoundEvents.BLOCK_NOTE_BLOCK_BASS.value();

        return new StopOnePerPlayerSoundS2CP(uuid, soundEvent);
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        if (MinecraftClient.getInstance().world != null) {
            @Nullable PlayerEntity sourcePlayer = MinecraftClient.getInstance().world.getPlayerByUuid(sourcePlayerId);
            if (sourcePlayer != null)
                MinecraftClient.getInstance().execute(() -> OnePerPlayerSounds.stop(sourcePlayer, soundEvent));
            else
                LogUtils.getLogger().debug("Cannot stop OnePerPlayer sound. SourcePlayer was not found by it's UUID.");
        }

        return true;
    }
}
