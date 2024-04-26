package io.github.mortuusars.exposure.camera.capture.component;

import io.github.mortuusars.exposure.camera.capture.Capture;
import java.awt.*;
import net.minecraft.util.math.MathHelper;

public class BlackAndWhiteComponent implements ICaptureComponent {
    @Override
    public Color modifyPixel(Capture capture, int red, int green, int blue) {
        // Weights adding up to more than 1 - to make the image slightly brighter
        int luma = MathHelper.clamp((int) (0.4 * red + 0.6 * green + 0.15 * blue), 0, 255);

        // Slightly increase the contrast
        int contrast = 136;
        luma = MathHelper.clamp((luma - 128) * contrast / 128 + 128, 0, 255);

        return new Color(luma, luma, luma);
    }
}
