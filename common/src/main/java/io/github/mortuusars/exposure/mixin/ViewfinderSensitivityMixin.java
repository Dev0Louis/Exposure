package io.github.mortuusars.exposure.mixin;

import io.github.mortuusars.exposure.camera.viewfinder.ViewfinderClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Mouse.class)
public abstract class ViewfinderSensitivityMixin {
    @ModifyVariable(method = "updateMouse", at = @At(value = "STORE"), ordinal = 3)
    private double modifySensitivity(double sensitivity) {
        return ViewfinderClient.modifyMouseSensitivity(sensitivity);
    }
}
