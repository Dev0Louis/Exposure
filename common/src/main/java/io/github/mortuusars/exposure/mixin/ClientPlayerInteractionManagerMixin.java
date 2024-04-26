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
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "interactEntityAtLocation", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/hit/EntityHitResult;getPos()Lnet/minecraft/util/math/Vec3d;"),
            cancellable = true)
    void onInteractAt(PlayerEntity player, Entity target, EntityHitResult ray, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ClientPlayerInteractionManager gameMode = MinecraftClient.getInstance().interactionManager;
        CameraInHand activeCamera = CameraInHand.getActive(player);
        if (gameMode != null && !activeCamera.isEmpty()) {
            gameMode.interactItem(player, activeCamera.getHand());
            cir.setReturnValue(ActionResult.CONSUME);
        }
    }

    @Inject(method = "interactBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/world/ClientWorld;getWorldBorder()Lnet/minecraft/world/border/WorldBorder;"),
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
