package io.github.mortuusars.exposure.mixin;

import io.github.mortuusars.exposure.client.KeyboardHandler;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "onKey", at = @At(value = "HEAD"), cancellable = true)
    private void keyPress(long windowPointer, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        if (KeyboardHandler.handleViewfinderKeyPress(windowPointer, key, scanCode, action, modifiers))
            ci.cancel();
    }
}
