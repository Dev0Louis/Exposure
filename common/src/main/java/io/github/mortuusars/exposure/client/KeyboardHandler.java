package io.github.mortuusars.exposure.client;

import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.camera.infrastructure.ZoomDirection;
import io.github.mortuusars.exposure.camera.viewfinder.SelfieClient;
import io.github.mortuusars.exposure.camera.viewfinder.ViewfinderClient;
import io.github.mortuusars.exposure.gui.ClientGUI;
import io.github.mortuusars.exposure.gui.screen.camera.ViewfinderControlsScreen;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.InputUtil;
import org.jetbrains.annotations.Nullable;

public class KeyboardHandler {
    public static boolean handleViewfinderKeyPress(long windowId, int key, int scanCode, int action, int modifiers) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        @Nullable ClientPlayerEntity player = minecraft.player;

        if (player == null || !CameraInHand.isActive(player))
            return false;

        if (minecraft.options.togglePerspectiveKey.matchesKey(key, scanCode)) {
            if (action == InputUtil.field_31997)
                return true;

            Perspective currentCameraType = minecraft.options.getPerspective();
            Perspective newCameraType = currentCameraType == Perspective.FIRST_PERSON ? Perspective.THIRD_PERSON_FRONT
                    : Perspective.FIRST_PERSON;

            minecraft.options.setPerspective(newCameraType);

            CameraInHand camera = CameraInHand.getActive(player);

            SelfieClient.update(camera.getCamera(), camera.getHand(), true);

            return true;
        }


        if (key == InputUtil.GLFW_KEY_ESCAPE || minecraft.options.inventoryKey.matchesKey(key, scanCode)) {
            if (action == 0) { // Release
                if (minecraft.currentScreen instanceof ViewfinderControlsScreen viewfinderControlsScreen)
                    viewfinderControlsScreen.close();
                else
                    CameraInHand.deactivate(player);
            }
            return true;
        }

        if (!ViewfinderClient.isLookingThrough())
            return false;

        if (!(minecraft.currentScreen instanceof ViewfinderControlsScreen)) {
            if (ExposureClient.getViewfinderControlsKey().matchesKey(key, scanCode)) {
                ClientGUI.openViewfinderControlsScreen();
                return false; // Do not handle to keep sneaking
            }

            if (action == 1 || action == 2) { // Press or Hold
                if (key == InputUtil.GLFW_KEY_KP_ADD || key == InputUtil.GLFW_KEY_EQUAL) {
                    ViewfinderClient.zoom(ZoomDirection.IN, false);
                    return true;
                }

                if (key == 333 /*KEY_SUBTRACT*/ || key == InputUtil.GLFW_KEY_MINUS) {
                    ViewfinderClient.zoom(ZoomDirection.OUT, false);
                    return true;
                }
            }
        }

        return false;
    }
}
