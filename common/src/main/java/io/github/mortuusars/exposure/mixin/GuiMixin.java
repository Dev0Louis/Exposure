package io.github.mortuusars.exposure.mixin;

import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.camera.viewfinder.ViewfinderClient;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.item.StackedPhotographsItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    private void renderGui(DrawContext guiGraphics, float partialTick, CallbackInfo ci) {
        if (ViewfinderClient.isLookingThrough())
            ci.cancel();
    }

    @Inject(method = "renderCrosshair", at = @At(value = "HEAD"), cancellable = true)
    private void renderCrosshair(DrawContext guiGraphics, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (Config.Client.PHOTOGRAPH_IN_HAND_HIDE_CROSSHAIR.get() && mc.player != null && mc.player.getPitch() > 25f
                && (mc.player.getMainHandStack().getItem() instanceof PhotographItem || mc.player.getMainHandStack().getItem() instanceof StackedPhotographsItem)
                && mc.player.getOffHandStack().isEmpty())
            ci.cancel();
    }
}
