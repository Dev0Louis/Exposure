package io.github.mortuusars.exposure.sound;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.sound.instance.ShutterTimerTickingSoundInstance;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.EntityTrackingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class OnePerPlayerSoundsClient {
    private static final Map<PlayerEntity, List<SoundInstance>> instances = new HashMap<>();

    public static void play(PlayerEntity sourcePlayer, SoundEvent soundEvent, SoundCategory source, float volume, float pitch) {
        World level = sourcePlayer.getWorld();
        stop(sourcePlayer, soundEvent);

        SoundInstance soundInstance = createSoundInstance(sourcePlayer, soundEvent, source, volume, pitch, level);

        List<SoundInstance> playingSounds = Optional.ofNullable(instances.get(sourcePlayer)).orElse(new ArrayList<>());
        playingSounds.add(soundInstance);
        instances.put(sourcePlayer, playingSounds);

        MinecraftClient.getInstance().getSoundManager().play(soundInstance);
    }

    public static void stop(PlayerEntity sourcePlayer, SoundEvent soundEvent) {
        if (instances.containsKey(sourcePlayer)) {
            Identifier soundLocation = soundEvent.getId();
            List<SoundInstance> playingSounds = instances.remove(sourcePlayer);
            for (int i = playingSounds.size() - 1; i >= 0; i--) {
                SoundInstance soundInstance = playingSounds.get(i);
                if (soundInstance.getId().equals(soundLocation)) {
                    MinecraftClient.getInstance().getSoundManager().stop(soundInstance);
                    playingSounds.remove(i);
                }
            }

            instances.put(sourcePlayer, playingSounds);
        }
    }


    @NotNull
    private static SoundInstance createSoundInstance(PlayerEntity sourcePlayer, SoundEvent soundEvent, SoundCategory source, float volume, float pitch, World level) {
        if (soundEvent == Exposure.SoundEvents.SHUTTER_TICKING.get())
            return new ShutterTimerTickingSoundInstance(sourcePlayer, soundEvent, source, volume, pitch, sourcePlayer.getWorld().getRandom());

        return new EntityTrackingSoundInstance(soundEvent, source, volume, pitch, sourcePlayer, level.getRandom().nextLong());
    }
}
