package io.github.mortuusars.exposure.mixin;

import io.github.mortuusars.exposure.gui.screen.camera.ViewfinderControlsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBinding.class)
public abstract class KeyMappingMixin {
    @Shadow
    private boolean pressed;

    /**
     * Allows moving when ControlsScreen is open.
     * This should also handle {@link net.minecraft.client.option.StickyKeyBinding} on fabric (forge has separate mixin for it).
     */
    @Inject(method = "isPressed", at = @At(value = "HEAD"), cancellable = true)
    private void isDown(CallbackInfoReturnable<Boolean> cir) {
        if (MinecraftClient.getInstance().currentScreen instanceof ViewfinderControlsScreen)
            cir.setReturnValue(this.pressed);
    }
}
