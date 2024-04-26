package io.github.mortuusars.exposure.camera.capture.component;

import io.github.mortuusars.exposure.camera.capture.Capture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.texture.NativeImage;

public class BaseComponent implements ICaptureComponent {
    private final boolean hideGuiValue;
    private boolean storedGuiHidden;
    private Perspective storedCameraType;

    public BaseComponent(boolean hideGuiOnCapture) {
        this.hideGuiValue = hideGuiOnCapture;
        storedGuiHidden = false;
        storedCameraType = Perspective.FIRST_PERSON;
    }

    public BaseComponent() {
        this(true);
    }

    @Override
    public void onDelayFrame(Capture capture, int delayFramesLeft) {
        if (delayFramesLeft == 0) { // Right before capturing
            MinecraftClient mc = MinecraftClient.getInstance();
            storedGuiHidden = mc.options.hudHidden;
            storedCameraType = mc.options.getPerspective();

            mc.options.hudHidden = hideGuiValue;
            Perspective cameraType = MinecraftClient.getInstance().options.getPerspective()
                    == Perspective.THIRD_PERSON_FRONT ? Perspective.THIRD_PERSON_FRONT : Perspective.FIRST_PERSON;
            mc.options.setPerspective(cameraType);
        }
    }

    @Override
    public void screenshotTaken(Capture capture, NativeImage screenshot) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.options.hudHidden = storedGuiHidden;
        mc.options.setPerspective(storedCameraType);
    }
}
