package io.github.mortuusars.exposure.network.handler;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.ExposureServer;
import io.github.mortuusars.exposure.camera.capture.Capture;
import io.github.mortuusars.exposure.camera.capture.CaptureManager;
import io.github.mortuusars.exposure.camera.capture.CapturedFramesHistory;
import io.github.mortuusars.exposure.camera.capture.component.BaseComponent;
import io.github.mortuusars.exposure.camera.capture.component.ExposureExporterComponent;
import io.github.mortuusars.exposure.camera.capture.component.ExposureStorageSaveComponent;
import io.github.mortuusars.exposure.camera.capture.component.ICaptureComponent;
import io.github.mortuusars.exposure.camera.capture.converter.DitheringColorConverter;
import io.github.mortuusars.exposure.camera.capture.converter.SimpleColorConverter;
import io.github.mortuusars.exposure.camera.infrastructure.FrameData;
import io.github.mortuusars.exposure.data.Lenses;
import io.github.mortuusars.exposure.data.ExposureSize;
import io.github.mortuusars.exposure.gui.screen.NegativeExposureScreen;
import io.github.mortuusars.exposure.gui.screen.PhotographScreen;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.network.packet.client.*;
import io.github.mortuusars.exposure.render.ExposureRenderer;
import io.github.mortuusars.exposure.render.PhotographRenderer;
import io.github.mortuusars.exposure.render.modifiers.ExposurePixelModifiers;
import io.github.mortuusars.exposure.util.ClientsideWorldNameGetter;
import io.github.mortuusars.exposure.util.ColorUtils;
import io.github.mortuusars.exposure.util.ItemAndStack;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.StringHelper;
import net.minecraft.util.Util;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClientPacketsHandler {
    public static void applyShader(ApplyShaderS2CP packet) {
        executeOnMainThread(() -> {
            if (packet.shaderLocation().getPath().equals("none")) {
                MinecraftClient.getInstance().gameRenderer.disablePostProcessor();
            } else {
                MinecraftClient.getInstance().gameRenderer.loadPostProcessor(packet.shaderLocation());
            }
        });
    }

    public static void exposeScreenshot(int size) {
        Preconditions.checkState(size > 0, size + " size is invalid. Should be larger than 0.");
        if (size == Integer.MAX_VALUE)
            size = Math.min(MinecraftClient.getInstance().getWindow().getFramebufferWidth(), MinecraftClient.getInstance().getWindow()
                    .getFramebufferHeight());

        int finalSize = size;
        executeOnMainThread(() -> {
            String filename = Util.getFormattedCurrentTime();
            NbtCompound frameData = new NbtCompound();
            frameData.putString(FrameData.ID, filename);
            Capture capture = new Capture(filename, frameData)
                    .size(finalSize)
                    .cropFactor(1f)
                    .components(
                            new BaseComponent(true),
                            new ExposureExporterComponent(filename)
                                    .organizeByWorld(Config.Client.EXPOSURE_SAVING_LEVEL_SUBFOLDER.get(),
                                            ClientsideWorldNameGetter::getWorldName)
                                    .withModifier(ExposurePixelModifiers.EMPTY)
                                    .withSize(ExposureSize.X1),
                            new ICaptureComponent() {
                                @Override
                                public void end(Capture capture) {
                                    LogUtils.getLogger().info("Saved exposure screenshot: " + filename);
                                }
                            })
                    .converter(new DitheringColorConverter());
            CaptureManager.enqueue(capture);
        });
    }

    public static void loadExposure(String exposureId, String path, int size, boolean dither) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (StringHelper.isEmpty(exposureId)) {
            if (player == null)
                throw new IllegalStateException("Cannot load exposure: path is null or empty and player is null.");
            exposureId = player.getName().getString() + player.getWorld().getTime();
        }

        String finalExposureId = exposureId;
        new Thread(() -> {
            try {
                BufferedImage read = ImageIO.read(new File(path));

                NativeImage image = new NativeImage(read.getWidth(), read.getHeight(), false);

                for (int x = 0; x < read.getWidth(); x++) {
                    for (int y = 0; y < read.getHeight(); y++) {
                        image.setColor(x, y, ColorUtils.BGRtoRGB(read.getRGB(x, y)));
                    }
                }

                NbtCompound frameData = new NbtCompound();
                frameData.putString(FrameData.ID, finalExposureId);

                Capture capture = new Capture(finalExposureId, frameData)
                        .size(size)
                        .cropFactor(1f)
                        .components(new ExposureStorageSaveComponent(finalExposureId, true))
                        .converter(dither ? new DitheringColorConverter() : new SimpleColorConverter());
                capture.processImage(image);

                LogUtils.getLogger()
                        .info("Loaded exposure from file '" + path + "' with Id: '" + finalExposureId + "'.");
                Objects.requireNonNull(MinecraftClient.getInstance().player).sendMessage(
                        Text.translatable("command.exposure.load_from_file.success", finalExposureId)
                                .formatted(Formatting.GREEN), false);
            } catch (IOException e) {
                LogUtils.getLogger().error("Cannot load exposure:" + e);
                Objects.requireNonNull(MinecraftClient.getInstance().player).sendMessage(
                        Text.translatable("command.exposure.load_from_file.failure")
                                .formatted(Formatting.RED), false);
            }
        }).start();
    }

    public static void startExposure(StartExposureS2CP packet) {
        MinecraftClient.getInstance().execute(() -> {
            @Nullable ClientPlayerEntity player = MinecraftClient.getInstance().player;
            Preconditions.checkState(player != null, "Player cannot be null.");

            ItemStack itemInHand = player.getStackInHand(packet.activeHand());
            if (!(itemInHand.getItem() instanceof CameraItem cameraItem) || !cameraItem.isActive(itemInHand))
                throw new IllegalStateException("Player should have active Camera in hand. " + itemInHand);

            cameraItem.exposeFrameClientside(player, packet.activeHand(), packet.exposureId(), packet.flashHasFired(), packet.lightLevel());
        });
    }

    public static void showExposure(ShowExposureS2CP packet) {
        executeOnMainThread(() -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) {
                LogUtils.getLogger().error("Cannot show exposures. Player is null.");
                return;
            }

            boolean negative = packet.negative();

            @Nullable Screen screen;

            if (packet.latest()) {
                screen = createLatestScreen(player, negative);
            } else {
                if (negative) {
                    Either<String, Identifier> idOrTexture = packet.isTexture() ?
                            Either.right(new Identifier(packet.idOrPath())) : Either.left(packet.idOrPath());
                    screen = new NegativeExposureScreen(List.of(idOrTexture));
                } else {
                    ItemStack stack = new ItemStack(Exposure.Items.PHOTOGRAPH.get());
                    NbtCompound tag = new NbtCompound();
                    tag.putString(packet.isTexture() ? FrameData.TEXTURE : FrameData.ID, packet.idOrPath());
                    stack.setNbt(tag);

                    screen = new PhotographScreen(List.of(new ItemAndStack<>(stack)));
                }
            }

            if (screen != null)
                MinecraftClient.getInstance().setScreen(screen);
        });
    }

    private static @Nullable Screen createLatestScreen(PlayerEntity player, boolean negative) {
        List<NbtCompound> latestFrames = CapturedFramesHistory.get()
                .stream()
                .filter(frame -> !frame.getString(FrameData.ID).isEmpty())
                .toList();

        if (latestFrames.isEmpty()) {
            player.sendMessage(Text.translatable("command.exposure.show.latest.error.no_exposures"), false);
            return null;
        }

        if (negative) {
            List<Either<String, Identifier>> exposures = new ArrayList<>();
            for (NbtCompound frame : latestFrames) {
                String exposureId = frame.getString(FrameData.ID);
                exposures.add(Either.left(exposureId));
            }
            return new NegativeExposureScreen(exposures);
        } else {
            List<ItemAndStack<PhotographItem>> photographs = new ArrayList<>();

            for (NbtCompound frame : latestFrames) {
                ItemStack stack = new ItemStack(Exposure.Items.PHOTOGRAPH.get());
                stack.setNbt(frame);

                photographs.add(new ItemAndStack<>(stack));
            }

            return new PhotographScreen(photographs);
        }
    }

    public static void clearRenderingCache() {
        executeOnMainThread(() -> ExposureClient.getExposureRenderer().clearData());
    }

    public static void syncLenses(SyncLensesS2CP packet) {
        executeOnMainThread(() -> Lenses.reload(packet.lenses()));
    }

    public static void waitForExposureChange(WaitForExposureChangeS2CP packet) {
        executeOnMainThread(() -> ExposureClient.getExposureStorage().putOnWaitingList(packet.exposureId()));
    }

    public static void onExposureChanged(ExposureChangedS2CP packet) {
        executeOnMainThread(() -> {
            ExposureClient.getExposureStorage().remove(packet.exposureId());
            ExposureClient.getExposureRenderer().clearDataSingle(packet.exposureId(), true);
        });
    }

    private static void executeOnMainThread(Runnable runnable) {
        MinecraftClient.getInstance().execute(runnable);
    }
}
