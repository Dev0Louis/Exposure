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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public record PlayOnePerPlayerSoundS2CP(UUID sourcePlayerId, SoundEvent soundEvent, SoundCategory source,
                                        float volume, float pitch) implements IPacket {
    public static final Identifier ID = Exposure.id("play_one_per_player_sound");

    @Override
    public Identifier getId() {
        return ID;
    }

    public PacketByteBuf toBuffer(PacketByteBuf buffer) {
        buffer.writeUuid(sourcePlayerId);
        buffer.writeIdentifier(soundEvent.getId());
        buffer.writeEnumConstant(source);
        buffer.writeFloat(volume);
        buffer.writeFloat(pitch);
        return buffer;
    }

    public static PlayOnePerPlayerSoundS2CP fromBuffer(PacketByteBuf buffer) {
        UUID uuid = buffer.readUuid();
        Identifier soundEventLocation = buffer.readIdentifier();
        @Nullable SoundEvent soundEvent = Registries.SOUND_EVENT.get(soundEventLocation);
        if (soundEvent == null)
            soundEvent = SoundEvents.BLOCK_NOTE_BLOCK_BASS.value();

        return new PlayOnePerPlayerSoundS2CP(uuid, soundEvent, buffer.readEnumConstant(SoundCategory.class),
                buffer.readFloat(), buffer.readFloat());
    }

    @Override
    public boolean handle(PacketDirection direction, @Nullable PlayerEntity player) {
        if (MinecraftClient.getInstance().world != null) {
            @Nullable PlayerEntity sourcePlayer = MinecraftClient.getInstance().world.getPlayerByUuid(sourcePlayerId);
            if (sourcePlayer != null)
                MinecraftClient.getInstance().execute(() -> OnePerPlayerSounds.play(sourcePlayer, soundEvent, source, volume, pitch));
            else
                LogUtils.getLogger().debug("Cannot play OnePerPlayer sound. SourcePlayer was not found by it's UUID.");
        }

        return true;
    }
}
