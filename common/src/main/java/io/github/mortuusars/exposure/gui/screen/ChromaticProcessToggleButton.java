package io.github.mortuusars.exposure.gui.screen;

import io.github.mortuusars.exposure.block.entity.Lightroom;
import java.util.function.Supplier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChromaticProcessToggleButton extends TexturedButtonWidget {
    private final Supplier<Lightroom.Process> processGetter;

    public ChromaticProcessToggleButton(int x, int y, PressAction onPress, Supplier<Lightroom.Process> processGetter) {
        super(x, y, 18, 18, 198, 17, 18,
                LightroomScreen.MAIN_TEXTURE, 256, 256, onPress);
        this.processGetter = processGetter;
    }

    @Override
    public void renderWidget(DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        Lightroom.Process currentProcess = processGetter.get();
        int xTex = currentProcess == Lightroom.Process.CHROMATIC ? 18 : 0;

        this.drawTexture(guiGraphics, this.texture, this.getX(), this.getY(), this.u + xTex, this.v,
                this.hoveredVOffset, this.width, this.height, this.textureWidth, this.textureHeight);

        MutableText tooltip = Text.translatable("gui.exposure.lightroom.process." + currentProcess.asString());
        if (currentProcess == Lightroom.Process.CHROMATIC) {
            tooltip.append(ScreenTexts.LINE_BREAK);
            tooltip.append(Text.translatable("gui.exposure.lightroom.process.chromatic.info")
                    .formatted(Formatting.GRAY));
        }

        setTooltip(Tooltip.of(tooltip));
    }
}
