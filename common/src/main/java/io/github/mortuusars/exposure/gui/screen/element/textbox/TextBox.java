package io.github.mortuusars.exposure.gui.screen.element.textbox;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.exposure.util.Pos2i;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

public class TextBox extends ClickableWidget {
    public final TextRenderer font;
    public Supplier<String> textGetter;
    public Consumer<String> textSetter;
    public Predicate<String> textValidator = text -> text != null
            && getFont().getWrappedLinesHeight(text, width) + (text.endsWith("\n") ? getFont().fontHeight : 0) <= height;

    public HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;
    public int fontColor = 0xFF000000;
    public int fontUnfocusedColor = 0xFF000000;
    public int selectionColor = 0xFF0000FF;
    public int selectionUnfocusedColor = 0x880000FF;

    public final SelectionManager textFieldHelper;
    protected DisplayCache displayCache = new DisplayCache();
    protected int frameTick;
    protected long lastClickTime;
    protected int lastIndex = -1;

    public TextBox(@NotNull TextRenderer font, int x, int y, int width, int height,
                   Supplier<String> textGetter, Consumer<String> textSetter) {
        super(x, y, width, height, Text.empty());
        this.font = font;
        this.textGetter = textGetter;
        this.textSetter = textSetter;
        textFieldHelper = new SelectionManager(this::getText, this::setText,
                SelectionManager.makeClipboardGetter(MinecraftClient.getInstance()),
                SelectionManager.makeClipboardSetter(MinecraftClient.getInstance()),
                this::validateText);
    }

    public void tick() {
        ++frameTick;
    }

    public TextRenderer getFont() {
        return font;
    }

    public @NotNull String getText() {
        return textGetter.get();
    }

    public TextBox setText(@NotNull String text) {
        textSetter.accept(text);
        clearDisplayCache();
        return this;
    }

    protected boolean validateText(String text) {
        return textValidator.test(text);
    }

    public void setHeight(int height) {
        this.height = height;
        clearDisplayCache();
    }

    public int getCurrentFontColor() {
        return isFocused() ? fontColor : fontUnfocusedColor;
    }

    public TextBox setFontColor(int fontColor, int fontUnfocusedColor) {
        this.fontColor = fontColor;
        this.fontUnfocusedColor = fontUnfocusedColor;
        clearDisplayCache();
        return this;
    }

    public TextBox setSelectionColor(int selectionColor, int selectionUnfocusedColor) {
        this.selectionColor = selectionColor;
        this.selectionUnfocusedColor = selectionUnfocusedColor;
        clearDisplayCache();
        return this;
    }

    public void setCursorToEnd() {
        textFieldHelper.putCursorAtEnd();
        clearDisplayCache();
    }

    public void refresh() {
        clearDisplayCache();
    }

    protected DisplayCache getDisplayCache() {
        if (displayCache.needsRebuilding)
            displayCache.rebuild(font, getText(), textFieldHelper.getSelectionStart(), textFieldHelper.getSelectionEnd(),
                    getX(), getY(), getWidth(), getHeight(), horizontalAlignment);
        return displayCache;
    }

    protected void clearDisplayCache() {
        displayCache.needsRebuilding = true;
    }

    protected Pos2i convertLocalToScreen(Pos2i pos) {
        return new Pos2i(getX() + pos.x, getY() + pos.y);
    }

    protected Pos2i convertScreenToLocal(Pos2i screenPos) {
        return new Pos2i(screenPos.x - getX(), screenPos.y - getY());
    }

    @Override
    protected void renderButton(DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        DisplayCache displayCache = this.getDisplayCache();
        for (DisplayCache.LineInfo lineInfo : displayCache.lines) {
            guiGraphics.drawText(this.font, lineInfo.asComponent, getX() + lineInfo.x, getY() + lineInfo.y, getCurrentFontColor(), false);
        }
        this.renderHighlight(guiGraphics, displayCache.selectionAreas);
        if (isFocused())
            this.renderCursor(guiGraphics, displayCache.cursorPos, displayCache.cursorAtEnd);
    }

    protected void renderHighlight(DrawContext guiGraphics, Rect2i[] highlightAreas) {
        for (Rect2i selection : highlightAreas) {
            int x = getX() + selection.getX();
            int y = getY() + selection.getY();
            int x1 = x + selection.getWidth();
            int y1 = y + selection.getHeight();
            guiGraphics.fill(RenderLayer.getGuiTextHighlight(), x, y - 1, x1, y1, isFocused() ? selectionColor : selectionUnfocusedColor);
        }
    }

    protected void renderCursor(DrawContext guiGraphics, Pos2i cursorPos, boolean isEndOfText) {
        if (this.frameTick / 6 % 2 == 0) {
            cursorPos = convertLocalToScreen(cursorPos);
            if (isEndOfText)
                guiGraphics.drawText(this.font, "_", cursorPos.x, cursorPos.y, getCurrentFontColor(), false);
            else {
                guiGraphics.getMatrices().push();
                guiGraphics.getMatrices().translate(0, 0, 50);
                RenderSystem.disableBlend();
                guiGraphics.fill(cursorPos.x, cursorPos.y - 1, cursorPos.x + 1, cursorPos.y + this.font.fontHeight, getCurrentFontColor());
                guiGraphics.getMatrices().pop();
            }
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder narrationElementOutput) {
        narrationElementOutput.put(NarrationPart.TITLE, getNarrationMessage());
    }

    @Override
    public @NotNull Text getMessage() {
        return Text.literal(getText());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused())
            return false;
        boolean handled = handleKeyPressed(keyCode, scanCode, modifiers);
        if (handled)
            clearDisplayCache();
        return handled;
    }

    protected boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        SelectionManager.SelectionType cursorStep = Screen.hasControlDown() ? SelectionManager.SelectionType.WORD : SelectionManager.SelectionType.CHARACTER;
        if (keyCode == InputUtil.GLFW_KEY_UP) {
            changeLine(-1);
            return true;
        } else if (keyCode == InputUtil.GLFW_KEY_DOWN) {
            changeLine(1);
            return true;
        } else if (keyCode == InputUtil.GLFW_KEY_HOME) {
            keyHome();
            return true;
        } else if (keyCode == InputUtil.GLFW_KEY_END) {
            keyEnd();
            return true;
        } else if (keyCode == InputUtil.GLFW_KEY_BACKSPACE) {
            textFieldHelper.delete(-1, cursorStep);
            return true;
        } else if (keyCode == InputUtil.GLFW_KEY_DELETE) {
            textFieldHelper.delete(1, cursorStep);
            return true;
        } else if (keyCode == InputUtil.GLFW_KEY_ENTER || keyCode == InputUtil.GLFW_KEY_KP_ENTER) {
            textFieldHelper.insert(ScreenTexts.LINE_BREAK.getString());
            return true;
        }

        return textFieldHelper.handleSpecialKey(keyCode);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!isFocused())
            return false;

        boolean typed = textFieldHelper.insert(codePoint);
        if (typed)
            clearDisplayCache();
        return typed;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hovered && visible && isNarratable() && button == 0) {
            long currentTime = Util.getMeasuringTimeMs();
            DisplayCache displayCache = getDisplayCache();
            int index = displayCache.getIndexAtPosition(font, convertScreenToLocal(new Pos2i((int) mouseX, (int) mouseY)));

            if (index >= 0) {
                if (index == lastIndex && currentTime - lastClickTime < 250L) {
                    if (!textFieldHelper.isSelecting()) {
                        selectWord(index);
                    } else {
                        textFieldHelper.selectAll();
                    }
                } else {
                    textFieldHelper.moveCursorTo(index, Screen.hasShiftDown());
                }
                clearDisplayCache();
            }

            lastIndex = index;
            lastClickTime = currentTime;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0) {
            DisplayCache displayCache = this.getDisplayCache();
            int index = displayCache.getIndexAtPosition(this.font, this.convertScreenToLocal(new Pos2i((int) mouseX, (int) mouseY)));
            this.textFieldHelper.moveCursorTo(index, true);
            this.clearDisplayCache();
        }
        return true;
    }

    protected void selectWord(int index) {
        String string = this.getText();
        this.textFieldHelper.setSelection(TextHandler.moveCursorByWords(string, -1, index, false),
                TextHandler.moveCursorByWords(string, 1, index, false));
    }

    protected void changeLine(int yChange) {
        int cursorPos = this.textFieldHelper.getSelectionStart();
        int line = this.getDisplayCache().changeLine(cursorPos, yChange);
        this.textFieldHelper.moveCursorTo(line, Screen.hasShiftDown());
    }

    protected void keyHome() {
        if (Screen.hasControlDown()) {
            this.textFieldHelper.moveCursorToStart(Screen.hasShiftDown());
        } else {
            int cursorIndex = this.textFieldHelper.getSelectionStart();
            int lineStartIndex = this.getDisplayCache().findLineStart(cursorIndex);
            this.textFieldHelper.moveCursorTo(lineStartIndex, Screen.hasShiftDown());
        }
    }

    protected void keyEnd() {
        if (Screen.hasControlDown()) {
            this.textFieldHelper.moveCursorToEnd(Screen.hasShiftDown());
        } else {
            DisplayCache displayCache = this.getDisplayCache();
            int cursorIndex = this.textFieldHelper.getSelectionStart();
            int lineEndIndex = displayCache.findLineEnd(cursorIndex);
            this.textFieldHelper.moveCursorTo(lineEndIndex, Screen.hasShiftDown());
        }
    }
}
