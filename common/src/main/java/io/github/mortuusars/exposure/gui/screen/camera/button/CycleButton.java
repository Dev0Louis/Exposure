package io.github.mortuusars.exposure.gui.screen.camera.button;

import io.github.mortuusars.exposure.gui.screen.element.IElementWithTooltip;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public abstract class CycleButton extends TexturedButtonWidget implements IElementWithTooltip {
    protected final Screen screen;
    protected int count = 1;
    protected int currentIndex = 0;
    protected boolean loop = true;

    public CycleButton(Screen screen, int x, int y, int width, int height, int u, int v, int yDiffTex, Identifier texture) {
        super(x, y, width, height, u, v, yDiffTex, texture, button -> {});
        this.screen = screen;
    }

    @Override
    public void renderButton(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderButton(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        super.render(guiGraphics, mouseX, mouseY, pPartialTick);
    }

    public void renderToolTip(@NotNull DrawContext guiGraphics, int mouseX, int mouseY) { }

    public void setupButtonElements(int count, int startingIndex) {
        this.count = count;
        this.currentIndex = startingIndex;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0 || button == 1) && clicked(mouseX, mouseY)) {
            cycle(button == 1);
            this.playDownSound(MinecraftClient.getInstance().getSoundManager());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        cycle(delta < 0d);
        this.playDownSound(MinecraftClient.getInstance().getSoundManager());
        return true;
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        boolean pressed = super.keyPressed(pKeyCode, pScanCode, pModifiers);

        if (pressed)
            cycle(Screen.hasShiftDown());

        return pressed;
    }

    protected void cycle(boolean reverse) {
        int value = currentIndex;
        value += reverse ? -1 : 1;
        if (value < 0)
            value = loop ? count - 1 : 0;
        else if (value >= count)
            value = loop ? 0 : count - 1;

        if (currentIndex != value) {
            currentIndex = value;
            onCycle();
        }
    }

    protected void onCycle() {

    }
}
