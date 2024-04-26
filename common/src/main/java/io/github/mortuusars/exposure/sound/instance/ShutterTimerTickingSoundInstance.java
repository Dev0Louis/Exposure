package io.github.mortuusars.exposure.sound.instance;

import io.github.mortuusars.exposure.item.CameraItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public class ShutterTimerTickingSoundInstance extends MovingSoundInstance {
    private final PlayerEntity player;
    private int delay = 2;
    private final float originalVolume;
    public ShutterTimerTickingSoundInstance(PlayerEntity player, SoundEvent soundEvent, SoundCategory soundSource, float volume, float pitch, Random random) {
        super(soundEvent, soundSource, random);
        this.player = player;
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.volume = volume;
        this.originalVolume = volume;
        this.pitch = pitch;
        this.repeat = true;
    }

    @Override
    public void tick() {
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();

        if (hasShutterOpen(player.getMainHandStack()) || hasShutterOpen(player.getOffHandStack())) {
            volume = MathHelper.lerp(0.3f, volume, originalVolume);
            return;
        }
        else
            volume = MathHelper.lerp(0.2f, volume, originalVolume * 0.3f);

        if (!hasCameraWithOpenShutterInInventory(player)) {
            // In multiplayer other players camera photo is not updated in time (sometimes)
            // This causes the sound to stop instantly
            if (!player.equals(MinecraftClient.getInstance().player) && repeatDelay > 0) {
                repeatDelay--;
                return;
            }

            this.setDone();
        }
    }

    private boolean hasCameraWithOpenShutterInInventory(PlayerEntity player) {
        for (ItemStack stack : player.getInventory().main) {
            if (hasShutterOpen(stack))
                return true;
        }

        return false;
    }

    private boolean hasShutterOpen(ItemStack stack) {
        return stack.getItem() instanceof CameraItem cameraItem && cameraItem.isShutterOpen(stack);
    }
}
