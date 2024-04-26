package io.github.mortuusars.exposure.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.block.entity.Lightroom;
import io.github.mortuusars.exposure.camera.infrastructure.FilmType;
import io.github.mortuusars.exposure.item.DevelopedFilmItem;
import io.github.mortuusars.exposure.menu.LightroomMenu;
import io.github.mortuusars.exposure.util.GuiUtil;
import io.github.mortuusars.exposure.util.PagingDirection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FilmFrameInspectScreen extends ZoomableScreen {
    public static final Identifier TEXTURE = Exposure.resource("textures/gui/film_frame_inspect.png");
    public static final Identifier WIDGETS_TEXTURE = Exposure.resource("textures/gui/widgets.png");
    public static final int BG_SIZE = 78;
    public static final int FRAME_SIZE = 54;
    public static final int BUTTON_SIZE = 16;

    private final LightroomScreen lightroomScreen;
    private final LightroomMenu lightroomMenu;

    private TexturedButtonWidget previousButton;
    private TexturedButtonWidget nextButton;

    public FilmFrameInspectScreen(LightroomScreen lightroomScreen, LightroomMenu lightroomMenu) {
        super(Text.empty());
        this.lightroomScreen = lightroomScreen;
        this.lightroomMenu = lightroomMenu;
        zoom.minZoom = zoom.defaultZoom / (float) Math.pow(zoom.step, 2f);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private LightroomMenu getLightroomMenu() {
        return lightroomMenu;
    }

    @Override
    protected void init() {
        super.init();

        zoomFactor = (float) height / BG_SIZE;

        previousButton = new TexturedButtonWidget(0, (int) (height / 2f - BUTTON_SIZE / 2f), BUTTON_SIZE, BUTTON_SIZE,
                0, 0, BUTTON_SIZE, WIDGETS_TEXTURE, this::buttonPressed);
        nextButton = new TexturedButtonWidget(width - BUTTON_SIZE, (int) (height / 2f - BUTTON_SIZE / 2f), BUTTON_SIZE, BUTTON_SIZE,
                16, 0, BUTTON_SIZE, WIDGETS_TEXTURE, this::buttonPressed);

        addDrawableChild(previousButton);
        addDrawableChild(nextButton);
    }

    private void buttonPressed(ButtonWidget button) {
        if (button == previousButton)
            lightroomScreen.changeFrame(PagingDirection.PREVIOUS);
        else if (button == nextButton)
            lightroomScreen.changeFrame(PagingDirection.NEXT);
    }

    public void close() {
        MinecraftClient.getInstance().setScreen(lightroomScreen);
        if (client.player != null)
            client.player.playSound(Exposure.SoundEvents.CAMERA_LENS_RING_CLICK.get(), 1f, 0.7f);
    }

    @Override
    public void render(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        guiGraphics.getMatrices().push();
        guiGraphics.getMatrices().translate(0, 0, 500); // Otherwise exposure will overlap buttons
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.getMatrices().pop();

        if (zoom.targetZoom == zoom.minZoom) {
            close();
            return;
        }

        guiGraphics.getMatrices().push();

        guiGraphics.getMatrices().translate(x, y, 0);
        guiGraphics.getMatrices().translate(width / 2f, height / 2f, 0);
        guiGraphics.getMatrices().scale(scale, scale, scale);

        RenderSystem.setShaderTexture(0, TEXTURE);

        guiGraphics.getMatrices().translate(BG_SIZE / -2f, BG_SIZE / -2f, 0);

        GuiUtil.blit(guiGraphics.getMatrices(), 0, 0, BG_SIZE, BG_SIZE, 0, 0, 256, 256, 0);

        ItemStack filmStack = lightroomMenu.getSlot(Lightroom.FILM_SLOT).getStack();
        if (!(filmStack.getItem() instanceof DevelopedFilmItem film))
            return;

        FilmType negative = film.getType();

        RenderSystem.setShaderColor(negative.filmR, negative.filmG, negative.filmB, negative.filmA);

        GuiUtil.blit(guiGraphics.getMatrices(), 0, 0, BG_SIZE, BG_SIZE, 0, BG_SIZE, 256, 256, 0);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.getMatrices().translate(12, 12, 0);

        int currentFrame = getLightroomMenu().getSelectedFrame();
        @Nullable NbtCompound frame = getLightroomMenu().getFrameIdByIndex(currentFrame);
        if (frame != null)
            lightroomScreen.renderFrame(frame, guiGraphics.getMatrices(), 0, 0, FRAME_SIZE, 1f, negative);

        guiGraphics.getMatrices().pop();

        previousButton.visible = currentFrame != 0;
        previousButton.active = currentFrame != 0;
        nextButton.visible = currentFrame != getLightroomMenu().getTotalFrames() - 1;
        nextButton.active = currentFrame != getLightroomMenu().getTotalFrames() - 1;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == InputUtil.GLFW_KEY_ESCAPE || client.options.inventoryKey.matchesKey(keyCode, scanCode))
            zoom.set(0f);
        else if (client.options.leftKey.matchesKey(keyCode, scanCode) || keyCode == InputUtil.GLFW_KEY_LEFT)
            lightroomScreen.changeFrame(PagingDirection.PREVIOUS);
        else if (client.options.rightKey.matchesKey(keyCode, scanCode) || keyCode == InputUtil.GLFW_KEY_RIGHT)
            lightroomScreen.changeFrame(PagingDirection.NEXT);
        else
            return super.keyPressed(keyCode, scanCode, modifiers);

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handled)
            return true;

        if (button == 1) { // Right Click
            zoom.set(0f);
            return true;
        }

        return false;
    }
}
