package io.github.mortuusars.exposure.camera.viewfinder;


import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.camera.infrastructure.FocalRange;
import io.github.mortuusars.exposure.camera.infrastructure.SynchronizedCameraInHandActions;
import io.github.mortuusars.exposure.camera.infrastructure.ZoomDirection;
import io.github.mortuusars.exposure.data.Filters;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.util.CameraInHand;
import io.github.mortuusars.exposure.util.Fov;
import io.github.mortuusars.exposure.util.ItemAndStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class ViewfinderClient {
    public static final float ZOOM_STEP = 8f;
    public static final float ZOOM_PRECISE_MODIFIER = 0.25f;
    private static boolean isOpen;

    private static FocalRange focalRange = new FocalRange(18, 55);
    private static double targetFov = 90f;
    private static double currentFov = targetFov;
    private static boolean shouldRestoreFov;

    @Nullable
    private static String previousShaderEffect;

    public static boolean isOpen() {
        return isOpen;
    }

    public static boolean isLookingThrough() {
        return isOpen() && (MinecraftClient.getInstance().options.getPerspective() == Perspective.FIRST_PERSON
                || MinecraftClient.getInstance().options.getPerspective() == Perspective.THIRD_PERSON_FRONT);
    }

    public static void open() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        Preconditions.checkState(player != null, "Player should not be null");
        Preconditions.checkState(player.getWorld().isClient(), "This should be called only client-side.");

        if (isOpen())
            return;

        @Nullable Hand activeHand = CameraInHand.getActiveHand(player);
        Preconditions.checkState(activeHand != null, "Player should have active camera in hand.");

        ItemAndStack<CameraItem> camera = new ItemAndStack<>(player.getStackInHand(activeHand));

        focalRange = camera.getItem().getFocalRange(camera.getStack());
        targetFov = Fov.focalLengthToFov(MathHelper.clamp(camera.getItem().getFocalLength(camera.getStack()), focalRange.min(), focalRange.max()));

        isOpen = true;

        camera.getItem().getAttachment(camera.getStack(), CameraItem.FILTER_ATTACHMENT)
                .flatMap(Filters::getShaderOf)
                .ifPresent(shaderLocation -> {
                    PostEffectProcessor effect = MinecraftClient.getInstance().gameRenderer.getPostProcessor();
                    if (effect != null)
                        previousShaderEffect = effect.getName();

                    MinecraftClient.getInstance().gameRenderer.loadPostProcessor(shaderLocation);
                });

//        Optional<ItemStack> attachment = camera.getItem().getAttachment(camera.getStack(), CameraItem.FILTER_ATTACHMENT);
//        attachment.ifPresent(stack -> {
//            Filters.getShaderOf(stack);
//
//            PostChain effect = Minecraft.getInstance().gameRenderer.currentEffect();
//            if (effect != null)
//                previousShaderEffect = effect.getName();
//
//
//
//            String itemName = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
//            Minecraft.getInstance().gameRenderer.loadEffect(Exposure.resource("shaders/post/" + itemName + ".json"));
//        });

        SelfieClient.update(camera, activeHand, false);

        ViewfinderOverlay.setup();
    }

    public static void close() {
        isOpen = false;
        targetFov = MinecraftClient.getInstance().options.getFov().getValue();

        MinecraftClient.getInstance().gameRenderer.disablePostProcessor();

        if (shouldRestorePreviousShaderEffect() && previousShaderEffect != null)
            MinecraftClient.getInstance().gameRenderer.loadPostProcessor(new Identifier(previousShaderEffect));

        previousShaderEffect = null;
    }

    private static boolean shouldRestorePreviousShaderEffect() {
        /*
            Cold Sweat applies a shader effect when having high temperature.
            If we restore effect after exiting viewfinder it will apply blur even if temp is normal.
            Not restoring shader is fine, Cold Sweat will reapply it if needed.
         */
        if (PlatformHelper.isModLoaded("cold_sweat") && previousShaderEffect != null && previousShaderEffect.equals("minecraft:shaders/post/blobs2.json"))
            return false;
        else
            return previousShaderEffect != null;
    }

    public static FocalRange getFocalRange() {
        return focalRange;
    }

    public static double getCurrentFov() {
        return currentFov;
    }

    public static float getSelfieCameraDistance() {
        return 1.75f;
    }

    public static void zoom(ZoomDirection direction, boolean precise) {
        double step = ZOOM_STEP * (1f - MathHelper.clamp((focalRange.min() - currentFov) / focalRange.min(), 0.3f, 1f));
        double inertia = Math.abs(targetFov - currentFov) * 0.8f;
        double change = step + inertia;

        if (precise)
            change *= ZOOM_PRECISE_MODIFIER;

        double prevFov = targetFov;

        double fov = MathHelper.clamp(targetFov + (direction == ZoomDirection.IN ? -change : +change),
                Fov.focalLengthToFov(focalRange.max()),
                Fov.focalLengthToFov(focalRange.min()));

        if (Math.abs(prevFov - fov) > 0.01f)
            Objects.requireNonNull(MinecraftClient.getInstance().player).playSoundIfNotSilent(Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get());

        targetFov = fov;
        SynchronizedCameraInHandActions.setZoom(Fov.fovToFocalLength(fov));
    }

    public static double modifyMouseSensitivity(double sensitivity) {
        if (!isLookingThrough())
            return sensitivity;

        double modifier = MathHelper.clamp(1f - (Config.Client.VIEWFINDER_ZOOM_SENSITIVITY_MODIFIER.get()
                * ((MinecraftClient.getInstance().options.getFov().getValue() - currentFov) / 5f)), 0.01, 2f);
        return sensitivity * modifier;
    }

    public static void onPlayerTick(PlayerEntity player) {
        if (!player.equals(MinecraftClient.getInstance().player))
            return;

        boolean cameraActive = CameraInHand.isActive(player);
        if (cameraActive && !ViewfinderClient.isOpen())
            ViewfinderClient.open();
        else if (!cameraActive && ViewfinderClient.isOpen())
            ViewfinderClient.close();
    }

    public static boolean handleMouseScroll(ZoomDirection direction) {
        if (isLookingThrough()) {
            zoom(direction, false);
            return true;
        }

        return false;
    }

    public static double modifyFov(double fov) {
        if (isLookingThrough()) {
            currentFov = MathHelper.lerp(Math.min(0.6f * MinecraftClient.getInstance().getLastFrameDuration(), 0.6f), currentFov, targetFov);
            shouldRestoreFov = true;
            return currentFov;
        }
        else if (shouldRestoreFov && Math.abs(currentFov - fov) > 0.00001) {
            currentFov = MathHelper.lerp(Math.min(0.8f * MinecraftClient.getInstance().getLastFrameDuration(), 0.8f), currentFov, fov);
            return currentFov;
        } else {
            currentFov = fov;
            shouldRestoreFov = false;
            return fov;
        }
    }
}
