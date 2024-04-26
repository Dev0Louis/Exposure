package io.github.mortuusars.exposure.gui.screen.album;

import io.github.mortuusars.exposure.gui.screen.element.textbox.HorizontalAlignment;
import io.github.mortuusars.exposure.gui.screen.element.textbox.TextBox;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.server.AlbumSignC2SP;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class AlbumSigningScreen extends Screen {
    public static final int SELECTION_COLOR = 0xFF8888FF;
    public static final int SELECTION_UNFOCUSED_COLOR = 0xFFBBBBFF;

    @NotNull
    protected final PlayerEntity player;

    protected final Screen parentScreen;
    protected final Identifier texture;

    protected int imageWidth, imageHeight, leftPos, topPos, textureWidth, textureHeight;

    protected TextBox titleTextBox;
    protected TexturedButtonWidget signButton;
    protected TexturedButtonWidget cancelSigningButton;

    protected String titleText = "";

    public AlbumSigningScreen(Screen screen, Identifier texture, int textureWidth, int textureHeight) {
        super(Text.empty());
        this.parentScreen = screen;
        this.texture = texture;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;

        client = MinecraftClient.getInstance();
        player = Objects.requireNonNull(client.player);
    }

    @Override
    protected void init() {
        this.imageWidth = 149;
        this.imageHeight = 188;
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        // TITLE
        titleTextBox = new TextBox(textRenderer, leftPos + 21, topPos + 73, 108, 9,
                () -> titleText, text -> titleText = text)
                .setFontColor(0xFF856036, 0xFF856036)
                .setSelectionColor(SELECTION_COLOR, SELECTION_UNFOCUSED_COLOR);
        titleTextBox.textValidator = text -> text != null && textRenderer.getWrappedLinesHeight(text, 108) <= 9 && !text.contains("\n");
        titleTextBox.horizontalAlignment = HorizontalAlignment.CENTER;
        addDrawableChild(titleTextBox);

        // SIGN
        signButton = new TexturedButtonWidget(leftPos + 46, topPos + 110, 22, 22, 242, 188,
                22, texture, textureWidth, textureHeight,
                b -> signAlbum(), Text.translatable("gui.exposure.album.sign"));
        MutableText component = Text.translatable("gui.exposure.album.sign")
                .append("\n").append(Text.translatable("gui.exposure.album.sign.warning").formatted(Formatting.GRAY));
        signButton.setTooltip(Tooltip.of(component));
        addDrawableChild(signButton);

        // CANCEL
        cancelSigningButton = new TexturedButtonWidget(leftPos + 83, topPos + 111, 22, 22, 264, 188,
                22, texture, textureWidth, textureHeight,
                b -> cancelSigning(), Text.translatable("gui.exposure.album.cancel_signing"));
        cancelSigningButton.setTooltip(Tooltip.of(Text.translatable("gui.exposure.album.cancel_signing")));
        addDrawableChild(cancelSigningButton);

        setInitialFocus(titleTextBox);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void tick() {
        titleTextBox.tick();
    }

    private void updateButtons() {
        signButton.active = canSign();
    }

    protected boolean canSign() {
        return !titleText.isEmpty();
    }

    @Override
    public void render(DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        updateButtons();

        renderBackground(guiGraphics);
        guiGraphics.drawTexture(texture, leftPos, topPos, 0, 298,
                0, imageWidth, imageHeight, textureWidth, textureHeight);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        renderLabels(guiGraphics);
    }

    private void renderLabels(DrawContext guiGraphics) {
        MutableText component = Text.translatable("gui.exposure.album.enter_title");
        guiGraphics.drawText(textRenderer, component,  leftPos + 149 / 2 - textRenderer.getWidth(component) / 2, topPos + 50, 0xf5ebd0, false);

        component = Text.translatable("gui.exposure.album.by_author", player.getName());
        guiGraphics.drawText(textRenderer, component, leftPos + 149 / 2 - textRenderer.getWidth(component) / 2, topPos + 84, 0xc7b496, false);
    }

    protected void signAlbum() {
        if (canSign()) {
            Packets.sendToServer(new AlbumSignC2SP(titleText));
            this.close();
        }
    }

    protected void cancelSigning() {
        client.setScreen(parentScreen);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputUtil.GLFW_KEY_TAB)
            return super.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == InputUtil.GLFW_KEY_ESCAPE) {
            cancelSigning();
            return true;
        }

        if (titleTextBox.isFocused())
            return titleTextBox.keyPressed(keyCode, scanCode, modifiers);

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
