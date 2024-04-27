package io.github.mortuusars.exposure.gui.screen.element;

import io.github.mortuusars.exposure.gui.screen.element.textbox.HorizontalAlignment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class TextBlock extends ClickableWidget {
    public int fontColor = 0xFF000000;
    public boolean drawShadow = false;
    public HorizontalAlignment alignment = HorizontalAlignment.LEFT;

    private final TextRenderer font;
    private final Function<Style, Boolean> componentClickedHandler;

    private List<OrderedText> renderedLines;
    private List<OrderedText> tooltipLines;

    public TextBlock(TextRenderer font, int x, int y, int width, int height, Text message, Function<Style, Boolean> componentClickedHandler) {
        super(x, y, width, height, message);
        this.font = font;
        this.componentClickedHandler = componentClickedHandler;

        makeLines();
    }

    @Override
    public void setMessage(Text message) {
        super.setMessage(message);
        makeLines();
    }

    protected void makeLines() {
        Text text = getMessage();
        List<OrderedText> lines = font.wrapLines(text, getWidth());

        int availableLines = Math.min(lines.size(), height / font.fontHeight);

        List<OrderedText> visibleLines = new ArrayList<>();
        for (int i = 0; i < availableLines; i++) {
            OrderedText line = lines.get(i);

            if (i == availableLines - 1 && availableLines < lines.size()) {
                line = OrderedText.concat(line,
                        Text.literal("...").fillStyle(text.getStyle()).asOrderedText());
            }

            visibleLines.add(line);
        }

        List<OrderedText> hiddenLines = Collections.emptyList();
        if (availableLines < lines.size()) {
            hiddenLines = new ArrayList<>(lines.stream()
                    .skip(availableLines)
                    .toList());

            hiddenLines.set(0, OrderedText.concat(
                    OrderedText.styledForwardsVisitedString("...", text.getStyle()), hiddenLines.get(0)));
        }

        this.renderedLines = visibleLines;
        this.tooltipLines = hiddenLines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Style style = getClickedComponentStyleAt(mouseX, mouseY);
        return button == 0 && style != null && componentClickedHandler.apply(style);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder narrationElementOutput) {
        narrationElementOutput.put(NarrationPart.TITLE, getNarrationMessage());
    }

    @Override
    protected @NotNull MutableText getNarrationMessage() {
        return getMessage().copy();
    }

    @Override
    protected void renderWidget(DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (int i = 0; i < renderedLines.size(); i++) {
            OrderedText line = renderedLines.get(i);

            int x = getX() + alignment.align(getWidth(), font.getWidth(line));
            guiGraphics.drawText(font, line, x, getY() + font.fontHeight * i, fontColor, drawShadow);
        }

        if (isHovered()) {
            Style style = getClickedComponentStyleAt(mouseX, mouseY);
            if (style != null)
                guiGraphics.drawHoverEvent(this.font, style, mouseX, mouseY);
        }

        if (!tooltipLines.isEmpty() && isMouseOver(mouseX, mouseY))
            guiGraphics.drawTooltip(font, tooltipLines, HoveredTooltipPositioner.INSTANCE, mouseX, mouseY);
    }

    public @Nullable Style getClickedComponentStyleAt(double mouseX, double mouseY) {
        if (renderedLines.isEmpty())
            return null;

        int x = MathHelper.floor(mouseX - getX());
        int y = MathHelper.floor(mouseY - getY());

        if (x < 0 || y < 0 || x > getWidth() || y > getHeight())
            return null;

        int hoveredLine = y / font.fontHeight;

        if (hoveredLine >= renderedLines.size())
            return null;

        OrderedText line = renderedLines.get(hoveredLine);
        int lineStart = alignment.align(getWidth(), font.getWidth(line));

        if (x < lineStart)
            return null;


        return font.getTextHandler().getStyleAt(line, x - lineStart);
    }
}
