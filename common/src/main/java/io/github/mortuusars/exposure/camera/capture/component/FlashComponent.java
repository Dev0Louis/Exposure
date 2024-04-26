package io.github.mortuusars.exposure.camera.capture.component;

import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.camera.capture.Capture;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;

public class FlashComponent implements ICaptureComponent {
    @Override
    public int getTicksDelay(Capture capture) {
        return Config.Client.FLASH_CAPTURE_DELAY_TICKS.get();
    }

    @Override
    public void initialize(Capture capture) {
        int ticksDelay = capture.getTicksDelay();
        int framesDelay = capture.getFramesDelay();
        if (ticksDelay > 6) {
            LogUtils.getLogger().warn("Capture ticksDelay of '" + ticksDelay + "' can be too long for use with a flash. " +
                    "The flash might disappear in that time.");
        }
        if (framesDelay > 20) {
            LogUtils.getLogger().warn("Capture framesDelay of '" + ticksDelay + "' can be too long for use with a flash. " +
                    "The flash might disappear in that time.");
        }
    }

    @Override
    public void screenshotTaken(Capture capture, NativeImage screenshot) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null)
            return;

        CameraInHand cameraInHand = new CameraInHand(player);
        if (cameraInHand.isEmpty())
            return;

        cameraInHand.getItem().spawnClientsideFlashEffects(player, cameraInHand.getStack());
    }
}
