package io.github.mortuusars.exposure.gui.screen;

import io.github.mortuusars.exposure.gui.screen.element.textbox.HorizontalAlignment;
import io.github.mortuusars.exposure.gui.screen.element.textbox.TextBox;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class TextTestScreen extends Screen {

    private TextBox textBox;

    public TextTestScreen() {
        super(Text.empty());
    }

    private String text = "initial";

    @Override
    protected void init() {
        textBox = new TextBox(textRenderer, width / 2 - 50, height / 2 - 20, 100, 40, () -> text, t -> text = t);

        textBox.horizontalAlignment = HorizontalAlignment.RIGHT;


        addDrawableChild(textBox);
    }

    @Override
    public void tick() {
        textBox.tick();
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {

        renderBackground(guiGraphics);
        guiGraphics.fill(textBox.getX() - 4, textBox.getY() - 4,
                textBox.getX() + textBox.getWidth() + 4, textBox.getY() + textBox.getHeight() + 4,
                textBox.isHovered() ? 0xFFffedc5 : 0xFFfff9ec);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

//        guiGraphics.drawString(font, this.getFocused() != null ? this.getFocused().toString() : "NONE", 5, 5, 0xFFFFFFFF);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputUtil.GLFW_KEY_TAB) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return textBox.keyPressed(keyCode, scanCode,modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }
}
