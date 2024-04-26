package io.github.mortuusars.exposure.mixin;

import io.github.mortuusars.exposure.camera.viewfinder.ViewfinderClient;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class SelfieCameraMixin {
    @Inject(method = "clipToSpace", at = @At(value = "RETURN"), cancellable = true)
    private void getMaxZoom(double pStartingDistance, CallbackInfoReturnable<Double> cir) {
        if (ViewfinderClient.isLookingThrough())
            cir.setReturnValue(Math.min(ViewfinderClient.getSelfieCameraDistance(), cir.getReturnValue()));
    }
}
