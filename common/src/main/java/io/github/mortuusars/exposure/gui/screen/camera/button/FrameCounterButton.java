package io.github.mortuusars.exposure.gui.screen.camera.button;

import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.gui.screen.element.IElementWithTooltip;
import io.github.mortuusars.exposure.util.CameraInHand;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FrameCounterButton extends TexturedButtonWidget implements IElementWithTooltip {
    private final int secondaryFontColor;
    private final int mainFontColor;

    public FrameCounterButton(Screen screen, int x, int y, int width, int height, int u, int v, Identifier texture) {
        super(x, y, width, height, u, v, height, texture, 256, 256, button -> {}, Text.empty());
        secondaryFontColor = Config.Client.getSecondaryFontColor();
        mainFontColor = Config.Client.getMainFontColor();
    }

    @Override
    public void render(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        super.render(guiGraphics, mouseX, mouseY, pPartialTick);
    }

    @Override
    public void renderWidget(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float pPartialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, pPartialTick);

        CameraInHand camera = CameraInHand.getActive(MinecraftClient.getInstance().player);

        String text = camera.isEmpty() ? "-" : camera.getItem().getFilm(camera.getStack()).map(film -> {
            int exposedFrames = film.getItem().getExposedFrames(film.getStack()).size();
            int totalFrames = film.getItem().getMaxFrameCount(film.getStack());
            return exposedFrames + "/" + totalFrames;
        }).orElse("-");

        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int textWidth = font.getWidth(text);
        int xPos = 15 + (27 - textWidth) / 2;

        guiGraphics.drawText(font, text, getX() + xPos, getY() + 8, secondaryFontColor, false);
        guiGraphics.drawText(font, text, getX() + xPos, getY() + 7, mainFontColor, false);
    }

    public void renderToolTip(@NotNull DrawContext guiGraphics, int mouseX, int mouseY) {
        List<Text> components = new ArrayList<>();
        components.add(Text.translatable("gui.exposure.viewfinder.film_frame_counter.tooltip"));

        CameraInHand camera = CameraInHand.getActive(MinecraftClient.getInstance().player);
        if (!camera.isEmpty() && camera.getItem().getFilm(camera.getStack()).isEmpty()) {
            components.add(Text.translatable("gui.exposure.viewfinder.film_frame_counter.tooltip.no_film")
                    .fillStyle(Style.EMPTY.withColor(0xdd6357)));
        }

        guiGraphics.drawTooltip(MinecraftClient.getInstance().textRenderer, components, Optional.empty(), mouseX, mouseY);
    }
}
