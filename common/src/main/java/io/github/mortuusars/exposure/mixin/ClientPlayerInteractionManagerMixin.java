package io.github.mortuusars.exposure.mixin;

import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "interactAt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/EntityHitResult;getLocation()Lnet/minecraft/world/phys/Vec3;"),
            cancellable = true)
    void onInteractAt(PlayerEntity player, Entity target, EntityHitResult ray, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ClientPlayerInteractionManager gameMode = MinecraftClient.getInstance().interactionManager;
        CameraInHand activeCamera = CameraInHand.getActive(player);
        if (gameMode != null && !activeCamera.isEmpty()) {
            gameMode.interactItem(player, activeCamera.getHand());
            cir.setReturnValue(ActionResult.CONSUME);
        }
    }

    @Inject(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getWorldBorder()Lnet/minecraft/world/level/border/WorldBorder;"),
            cancellable = true)
    void onUseItemOn(ClientPlayerEntity player, Hand hand, BlockHitResult result, CallbackInfoReturnable<ActionResult> cir) {
        ClientPlayerInteractionManager gameMode = MinecraftClient.getInstance().interactionManager;
        CameraInHand activeCamera = CameraInHand.getActive(player);
        if (gameMode != null && !activeCamera.isEmpty()) {
            gameMode.interactItem(player, activeCamera.getHand());
            cir.setReturnValue(ActionResult.CONSUME);
        }
    }
}
