package io.github.mortuusars.exposure.client;

import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.gui.ClientGUI;
import io.github.mortuusars.exposure.gui.screen.camera.ViewfinderControlsScreen;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public class MouseHandler {
    private static final boolean[] heldMouseButtons = new boolean[12];

    public static boolean handleMouseButtonPress(int button, int action, int modifiers) {
        if (button >= 0 && button < heldMouseButtons.length)
            heldMouseButtons[button] = action == InputUtil.field_31997;

        if (MinecraftClient.getInstance().player != null && CameraInHand.isActive(MinecraftClient.getInstance().player)
                && ExposureClient.getViewfinderControlsKey().matchesMouse(button)
                && !(MinecraftClient.getInstance().currentScreen instanceof ViewfinderControlsScreen)) {
            ClientGUI.openViewfinderControlsScreen();
            // Do not cancel the event to keep sneaking
        }

        return false;
    }

    public static boolean isMouseButtonHeld(int button) {
        return button >= 0 && button < heldMouseButtons.length && heldMouseButtons[button];
    }
}
