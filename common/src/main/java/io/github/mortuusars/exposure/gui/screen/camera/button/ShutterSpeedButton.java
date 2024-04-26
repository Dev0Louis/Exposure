package io.github.mortuusars.exposure.gui.screen.camera.button;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.infrastructure.ShutterSpeed;
import io.github.mortuusars.exposure.camera.infrastructure.SynchronizedCameraInHandActions;
import io.github.mortuusars.exposure.util.CameraInHand;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ShutterSpeedButton extends CycleButton {
    private final List<ShutterSpeed> shutterSpeeds;
    private final int secondaryFontColor;
    private final int mainFontColor;

    public ShutterSpeedButton(Screen screen, int x, int y, int width, int height, int u, int v, Identifier texture) {
        super(screen, x, y, width, height, u, v, height, texture);

        CameraInHand camera = CameraInHand.getActive(MinecraftClient.getInstance().player);
        Preconditions.checkState(!camera.isEmpty(), "Player must hold an active camera at this point.");

        List<ShutterSpeed> speeds = new ArrayList<>(camera.getItem().getAllShutterSpeeds(camera.getStack()));
        Collections.reverse(speeds);
        shutterSpeeds = speeds;

        ShutterSpeed shutterSpeed = camera.getItem().getShutterSpeed(camera.getStack());
        if (!shutterSpeeds.contains(shutterSpeed)) {
            throw new IllegalStateException("Camera {" + camera.getStack() + "} has invalid shutter speed.");
        }

        int currentShutterSpeedIndex = 0;
        for (int i = 0; i < shutterSpeeds.size(); i++) {
            if (shutterSpeed.equals(shutterSpeeds.get(i)))
                currentShutterSpeedIndex = i;
        }

        setupButtonElements(shutterSpeeds.size(), currentShutterSpeedIndex);
        secondaryFontColor = Config.Client.getSecondaryFontColor();
        mainFontColor = Config.Client.getMainFontColor();
    }

    @Override
    public void playDownSound(SoundManager handler) {
        handler.play(PositionedSoundInstance.master(Exposure.SoundEvents.CAMERA_DIAL_CLICK.get(),
                Objects.requireNonNull(MinecraftClient.getInstance().world).random.nextFloat() * 0.05f + 0.9f + currentIndex * 0.01f, 0.7f));
    }

    @Override
    public void renderButton(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderButton(guiGraphics, mouseX, mouseY, partialTick);

        ShutterSpeed shutterSpeed = shutterSpeeds.get(currentIndex);
        String text = shutterSpeed.toString();

        if (shutterSpeed.equals(ShutterSpeed.DEFAULT))
            text = text + "•";

        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int textWidth = font.getWidth(text);
        int xPos = 35 - (textWidth / 2);

        guiGraphics.drawText(font, text, getX() + xPos, getY() + 4, secondaryFontColor, false);
        guiGraphics.drawText(font, text, getX() + xPos, getY() + 3, mainFontColor, false);
    }

    @Override
    public void renderToolTip(@NotNull DrawContext guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawTooltip(MinecraftClient.getInstance().textRenderer, Text.translatable("gui.exposure.viewfinder.shutter_speed.tooltip"), mouseX, mouseY);
    }

    @Override
    protected void onCycle() {
        CameraInHand camera = CameraInHand.getActive(MinecraftClient.getInstance().player);
        if (!camera.isEmpty()) {
            if (camera.getItem().getShutterSpeed(camera.getStack()) != shutterSpeeds.get(currentIndex)) {
                SynchronizedCameraInHandActions.setShutterSpeed(shutterSpeeds.get(currentIndex));
            }
        }
    }
}
