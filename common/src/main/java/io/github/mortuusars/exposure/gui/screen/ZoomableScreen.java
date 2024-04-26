package io.github.mortuusars.exposure.gui.screen;

import io.github.mortuusars.exposure.camera.infrastructure.ZoomDirection;
import io.github.mortuusars.exposure.gui.screen.element.ZoomHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;

public abstract class ZoomableScreen extends Screen {
    protected final ZoomHandler zoom = new ZoomHandler();
    protected float zoomFactor = 1f;
    protected float scale = 1f;
    protected float x;
    protected float y;

    @NotNull
    protected final MinecraftClient client = MinecraftClient.getInstance();

    protected ZoomableScreen(Text title) {
        super(title);
    }

    @Override
    public void render(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        zoom.update(partialTick);
        scale = zoom.get() * zoomFactor;
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        if (handled)
            return true;

        if (client.options.inventoryKey.matchesKey(keyCode, scanCode))
            this.close();
        else if (keyCode == InputUtil.GLFW_KEY_KP_ADD || keyCode == InputUtil.GLFW_KEY_EQUAL)
            zoom.change(ZoomDirection.IN);
        else if (keyCode == 333 /*KEY_SUBTRACT*/ || keyCode == InputUtil.GLFW_KEY_MINUS)
            zoom.change(ZoomDirection.OUT);
        else
            return false;

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        boolean handled = super.mouseScrolled(mouseX, mouseY, delta);

        if (!handled) {
            zoom.change(delta >= 0.0 ? ZoomDirection.IN : ZoomDirection.OUT);
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        boolean handled = super.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        if (!handled && button == 0) { // Left Click
            float centerX = width / 2f;
            float centerY = height / 2f;

            x = (float) MathHelper.clamp(x + dragX, -centerX, centerX);
            y = (float) MathHelper.clamp(y + dragY, -centerY, centerY);
            handled = true;
        }

        return handled;
    }
}
