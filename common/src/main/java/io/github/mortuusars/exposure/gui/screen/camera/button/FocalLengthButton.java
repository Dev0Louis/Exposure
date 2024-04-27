package io.github.mortuusars.exposure.gui.screen.camera.button;

import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.camera.viewfinder.ViewfinderClient;
import io.github.mortuusars.exposure.gui.screen.element.IElementWithTooltip;
import io.github.mortuusars.exposure.util.Fov;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class FocalLengthButton extends TexturedButtonWidget implements IElementWithTooltip {
    private final int secondaryFontColor;
    private final int mainFontColor;

    public FocalLengthButton(Screen screen, int x, int y, int width, int height, int u, int v, Identifier texture) {
        super(x, y, width, height, u, v, height, texture, 256, 256, button -> {}, Text.empty());
        secondaryFontColor = Config.Client.getSecondaryFontColor();
        mainFontColor = Config.Client.getMainFontColor();
    }

    @Override
    public void render(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        super.render(guiGraphics, mouseX, mouseY, pPartialTick);
    }

    public void renderToolTip(@NotNull DrawContext guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawTooltip(MinecraftClient.getInstance().textRenderer, Text.translatable("gui.exposure.viewfinder.focal_length.tooltip"), mouseX, mouseY);
    }

    @Override
    public void renderWidget(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

        int focalLength = (int)Math.round(Fov.fovToFocalLength(ViewfinderClient.getCurrentFov()));

        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        MutableText text = Text.translatable("gui.exposure.viewfinder.focal_length", focalLength);
        int textWidth = font.getWidth(text);
        int xPos = 17 + (29 - textWidth) / 2;

        guiGraphics.drawText(font, text, getX() + xPos, getY() + 8, secondaryFontColor, false);
        guiGraphics.drawText(font, text, getX() + xPos, getY() + 7, mainFontColor, false);
    }
}
