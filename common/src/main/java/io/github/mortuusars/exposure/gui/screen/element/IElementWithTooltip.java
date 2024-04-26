package io.github.mortuusars.exposure.gui.screen.element;

import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;

public interface IElementWithTooltip {
    void renderToolTip(@NotNull DrawContext guiGraphics, int mouseX, int mouseY);
}
