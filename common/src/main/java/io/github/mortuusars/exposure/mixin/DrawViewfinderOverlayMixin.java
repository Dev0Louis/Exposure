package io.github.mortuusars.exposure.mixin;

import io.github.mortuusars.exposure.camera.viewfinder.ViewfinderClient;
import io.github.mortuusars.exposure.camera.viewfinder.ViewfinderOverlay;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class DrawViewfinderOverlayMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target =  "Lnet/minecraft/client/MinecraftClient;getProfiler()Lnet/minecraft/util/profiler/Profiler;", ordinal = 1))
    private void renderViewfinder(float partialTicks, long nanoTime, boolean renderLevel, CallbackInfo ci) {
        if (ViewfinderClient.isLookingThrough())
            ViewfinderOverlay.render();
    }
}
