package io.github.mortuusars.exposure.util;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.server.DeactivateCamerasInHandC2SP;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class CameraInHand {
    public static final CameraInHand EMPTY = new CameraInHand(null, null);

    @Nullable
    private ItemAndStack<CameraItem> camera;
    @Nullable
    private Hand hand;

    public CameraInHand(@Nullable ItemAndStack<CameraItem> camera, @Nullable Hand hand) {
        this.camera = camera;
        this.hand = hand;
    }

    public CameraInHand(@NotNull PlayerEntity player) {
        for (Hand hand : Hand.values()) {
            ItemStack itemInHand = player.getStackInHand(hand);
            if (itemInHand.getItem() instanceof CameraItem) {
                this.camera = new ItemAndStack<>(itemInHand);
                this.hand = hand;
                return;
            }
        }
    }

    public static void deactivate(PlayerEntity player) {
        Preconditions.checkArgument(player != null, "Player cannot be null");
        for (Hand hand : Hand.values()) {
            ItemStack itemInHand = player.getStackInHand(hand);
            if (itemInHand.getItem() instanceof CameraItem cameraItem)
                cameraItem.deactivate(player, itemInHand);
        }

        if (player.getWorld().isClient)
            Packets.sendToServer(new DeactivateCamerasInHandC2SP());
    }

    public static @Nullable Hand getActiveHand(PlayerEntity player) {
        Preconditions.checkArgument(player != null, "Player should not be null.");

        for (Hand hand : Hand.values()) {
            ItemStack itemInHand = player.getStackInHand(hand);
            if (itemInHand.getItem() instanceof CameraItem cameraItem && cameraItem.isActive(itemInHand))
                return hand;
        }

        return null;
    }

    public static boolean isActive(PlayerEntity player) {
        return getActiveHand(player) != null;
    }

    public static CameraInHand getActive(PlayerEntity player) {
        @Nullable Hand activeHand = getActiveHand(player);
        if (activeHand == null)
            return EMPTY;

        return new CameraInHand(new ItemAndStack<>(player.getStackInHand(activeHand)), activeHand);
    }

    public boolean isEmpty() {
        return this.equals(EMPTY) || camera == null || hand == null;
    }

    public ItemAndStack<CameraItem> getCamera() {
        Preconditions.checkState(!isEmpty(), "getCamera should not be called before checking isEmpty first.");
        return camera;
    }

    public CameraItem getItem() {
        Preconditions.checkState(!isEmpty(), "getItem should not be called before checking isEmpty first.");
        Preconditions.checkState(camera != null, "getItem should not be called before checking isEmpty first.");
        return camera.getItem();
    }

    public ItemStack getStack() {
        Preconditions.checkState(!isEmpty(), "getStack should not be called before checking isEmpty first.");
        Preconditions.checkState(camera != null, "getStack should not be called before checking isEmpty first.");
        return camera.getStack();
    }

    public Hand getHand() {
        Preconditions.checkState(!isEmpty(), "getHand should not be called before checking isEmpty first.");
        return hand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CameraInHand that = (CameraInHand) o;
        return Objects.equals(camera, that.camera) && hand == that.hand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(camera, hand);
    }

    @Override
    public String toString() {
        return "CameraInHand{" +
                "camera=" + (camera != null ? camera.getStack() : "null") +
                ", hand=" + hand +
                '}';
    }
}
