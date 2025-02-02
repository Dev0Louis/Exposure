package io.github.mortuusars.exposure.camera.capture;

import com.google.common.base.Preconditions;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.camera.capture.component.ICaptureComponent;
import io.github.mortuusars.exposure.camera.capture.converter.IImageToMapColorsConverter;
import io.github.mortuusars.exposure.camera.capture.converter.SimpleColorConverter;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import io.github.mortuusars.exposure.util.ColorUtils;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;

@SuppressWarnings("unused")
public class Capture {
    private final String exposureId;
    private final NbtCompound frameData;
    private FilmType type = FilmType.COLOR;
    private int size = Exposure.DEFAULT_FILM_SIZE;
    private float cropFactor = Exposure.CROP_FACTOR;
    private float brightnessStops = 0f;
    private boolean asyncProcessing = true;
    private Collection<ICaptureComponent> components = Collections.emptyList();
    private IImageToMapColorsConverter converter = new SimpleColorConverter();

    private int ticksDelay = -1;
    private int framesDelay = -1;
    private long captureTick;
    private boolean completed = false;
    private long currentTick;

    public Capture(String exposureId, NbtCompound frameData) {
        this.exposureId = exposureId;
        this.frameData = frameData;
    }

    public boolean isCompleted() {
        return completed;
    }

    public int getTicksDelay() {
        return (int)(captureTick - Objects.requireNonNull(MinecraftClient.getInstance().world).getTime());
    }

    public int getFramesDelay() {
        return framesDelay;
    }

    public FilmType getFilmType() {
        return type;
    }

    public Capture setFilmType(FilmType type) {
        this.type = type;
        return this;
    }

    public int getSize() {
        return size;
    }

    public Capture size(int size) {
        Preconditions.checkArgument(size > 0, "'size' cannot be less or equal to 0.");
        this.size = size;
        return this;
    }

    public float getCropFactor() {
        return cropFactor;
    }

    public Capture cropFactor(float cropFactor) {
        Preconditions.checkArgument(cropFactor != 0, "'cropFactor' cannot be 0.");
        this.cropFactor = cropFactor;
        return this;
    }

    public float getBrightnessStops() {
        return brightnessStops;
    }

    public Capture brightnessStops(float brightnessStops) {
        this.brightnessStops = brightnessStops;
        return this;
    }

    public Capture setAsyncProcessing(boolean asyncProcessing) {
        this.asyncProcessing = asyncProcessing;
        return this;
    }

    public Capture components(ICaptureComponent... components) {
        this.components = List.of(components);
        return this;
    }

    public Capture components(Collection<ICaptureComponent> components) {
        this.components = components;
        return this;
    }

    public Capture converter(IImageToMapColorsConverter converter) {
        this.converter = converter;
        return this;
    }

    public void initialize() {
        for (ICaptureComponent modifier : components) {
            ticksDelay = Math.max(ticksDelay, modifier.getTicksDelay(this));
            framesDelay = Math.max(framesDelay, modifier.getFramesDelay(this));
        }

        for (ICaptureComponent modifier : components) {
            modifier.initialize(this);
        }

        currentTick = Objects.requireNonNull(MinecraftClient.getInstance().world).getTime();
        captureTick = currentTick + ticksDelay;

        if (currentTick == captureTick && framesDelay <= 0) {
            for (ICaptureComponent modifier : components) {
                modifier.onDelayTick(this, 0);
                modifier.onDelayFrame(this, 0);
            }
        }
    }

    public void tick() {
        long lastTick = currentTick;
        currentTick = Objects.requireNonNull(MinecraftClient.getInstance().world).getTime();

        if (ticksDelay > 0) {
            if (lastTick < currentTick) {
                ticksDelay--;

                for (ICaptureComponent modifier : components) {
                    modifier.onDelayTick(this, ticksDelay);
                }

                if (ticksDelay == 0 && framesDelay == 0) {
                    for (ICaptureComponent modifier : components) {
                        modifier.onDelayFrame(this, 0);
                    }
                }
            }

            return;
        }

        if (framesDelay > 0) {
            framesDelay--;

            for (ICaptureComponent modifier : components) {
                modifier.onDelayFrame(this, framesDelay);
            }

            return;
        }

        NativeImage screenshot = ScreenshotRecorder.takeScreenshot(MinecraftClient.getInstance().getFramebuffer());

        for (ICaptureComponent modifier : components) {
            modifier.screenshotTaken(this, screenshot);
        }

        if (asyncProcessing)
            new Thread(() -> processImage(screenshot), "ExposureProcessing").start();
        else
            processImage(screenshot);

        completed = true;
    }

    public void processImage(NativeImage screenshotImage) {
        try {
            BufferedImage image = scaleCropAndProcess(screenshotImage);

            for (ICaptureComponent component : components) {
                image = component.modifyImage(this, image);
            }

            byte[] pixels = converter.convert(this, image);

            for (ICaptureComponent component : components) {
                component.teardown(this);
            }

            NbtCompound properties = new NbtCompound();
            properties.putString(ExposureSavedData.TYPE_PROPERTY, getFilmType().asString());

            for (ICaptureComponent component : components) {
                component.save(pixels, image.getWidth(), image.getHeight(), properties);
            }

            CapturedFramesHistory.add(frameData);
        }
        catch (Exception e) {
            LogUtils.getLogger().error(e.toString());
        }
        finally {
            try {
                for (ICaptureComponent component : components) {
                    component.end(this);
                }
            } catch (Exception e) {
                LogUtils.getLogger().error(e.toString());
            }
        }
    }

    private BufferedImage scaleCropAndProcess(NativeImage sourceImage) {
        int sWidth = sourceImage.getWidth();
        int sHeight = sourceImage.getHeight();

        int sourceSize = Math.min(sWidth, sHeight);
        float crop = sourceSize - (sourceSize / getCropFactor());
        sourceSize -= crop;

        int sourceXStart = sWidth > sHeight ? (sWidth - sHeight) / 2 : 0;
        int sourceYStart = sHeight > sWidth ? (sHeight - sWidth) / 2 : 0;

        sourceXStart += crop / 2;
        sourceYStart += crop / 2;

        int size = getSize();

        BufferedImage bufferedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);

        try (sourceImage) {
            for (int x = 0; x < size; x++) {
                float sourceX = sourceSize * (x / (float) size);
                int sx = MathHelper.clamp((int) sourceX + sourceXStart, sourceXStart, sourceXStart + sourceSize);

                for (int y = 0; y < size; y++) {
                    float sourceY = sourceSize * (y / (float) size);
                    int sy = MathHelper.clamp((int) sourceY + sourceYStart, sourceYStart, sourceYStart + sourceSize);

                    int rgba = ColorUtils.BGRtoRGB(sourceImage.getColor(sx, sy)); // Mojang decided to return BGR in getPixelRGBA method.
                    Color pixel = new Color(rgba, false);

                    for (ICaptureComponent component : components) {
                        pixel = component.modifyPixel(this, pixel.getRed(), pixel.getGreen(), pixel.getBlue());
                    }

                    bufferedImage.setRGB(x, y, 0xFF << 24 | pixel.getRed() << 16 | pixel.getGreen() << 8 | pixel.getBlue());
                }
            }
        } catch (Exception e) {
            LogUtils.getLogger().error("Failed to create an image: " + e);
        }

        return bufferedImage;
    }
}
