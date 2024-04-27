package io.github.mortuusars.exposure.gui.screen.camera;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.camera.infrastructure.ZoomDirection;
import io.github.mortuusars.exposure.camera.viewfinder.ViewfinderClient;
import io.github.mortuusars.exposure.camera.viewfinder.ViewfinderOverlay;
import io.github.mortuusars.exposure.client.MouseHandler;
import io.github.mortuusars.exposure.gui.screen.element.IElementWithTooltip;
import io.github.mortuusars.exposure.gui.screen.camera.button.*;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.util.CameraInHand;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public class ViewfinderControlsScreen extends Screen {
    public static final Identifier TEXTURE = Exposure.id("textures/gui/viewfinder/viewfinder_camera_controls.png");

    private final PlayerEntity player;
    private final ClientWorld level;
    private final long openedAtTimestamp;

    public ViewfinderControlsScreen() {
        super(Text.empty());

        player = MinecraftClient.getInstance().player;
        level = MinecraftClient.getInstance().world;
        assert level != null;
        openedAtTimestamp = level.getTime();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void tick() {
        refreshMovementKeys();
        MinecraftClient.getInstance().handleInputEvents();
    }

    @Override
    protected void init() {
        super.init();
        refreshMovementKeys();

        int leftPos = (width - 256) / 2;
        int topPos = Math.round(ViewfinderOverlay.opening.y + ViewfinderOverlay.opening.height - 256);

        CameraInHand camera = CameraInHand.getActive(player);
        Preconditions.checkState(!camera.isEmpty(), "Player should hold an Active Camera at this point.");

        boolean hasFlashAttached = camera.getItem().getAttachment(camera.getStack(), CameraItem.FLASH_ATTACHMENT).isPresent();

        int sideButtonsWidth = 48;
        int buttonWidth = 15;

        int elementX = leftPos + 128 - (sideButtonsWidth + 1 + buttonWidth + 1 + (hasFlashAttached ? buttonWidth + 1 : 0) + sideButtonsWidth) / 2;
        int elementY = topPos + 238;

        // Order of adding influences TAB key behavior

        ShutterSpeedButton shutterSpeedButton = new ShutterSpeedButton(this, leftPos + 94, topPos + 226, 69, 12, 112, 0, TEXTURE);
        addDrawableChild(shutterSpeedButton);

        FocalLengthButton focalLengthButton = new FocalLengthButton(this, elementX, elementY, 48, 18, 0, 0, TEXTURE);
        addDrawable(focalLengthButton);
        elementX += focalLengthButton.getWidth();

        TexturedButtonWidget separator1 = new TexturedButtonWidget(elementX, elementY, 1, 18, 111, 0, TEXTURE, pButton -> {});
        addDrawable(separator1);
        elementX += separator1.getWidth();

        CompositionGuideButton compositionGuideButton = new CompositionGuideButton(this, elementX, elementY, 15, 18, 48, 0, TEXTURE);
        addDrawableChild(compositionGuideButton);
        elementX += compositionGuideButton.getWidth();

        TexturedButtonWidget separator2 = new TexturedButtonWidget(elementX, elementY, 1, 18, 111, 0, TEXTURE, pButton -> {});
        addDrawable(separator2);
        elementX += separator2.getWidth();

        if (hasFlashAttached) {
            FlashModeButton flashModeButton = new FlashModeButton(this, elementX, elementY, 15, 18, 48, 0, TEXTURE);
            addDrawableChild(flashModeButton);
            elementX += flashModeButton.getWidth();

            TexturedButtonWidget separator3 = new TexturedButtonWidget(elementX, elementY, 1, 18, 111, 0, TEXTURE, pButton -> {});
            addDrawable(separator3);
            elementX += separator3.getWidth();
        }

        FrameCounterButton frameCounterButton = new FrameCounterButton(this, elementX, elementY, 48, 18, 63, 0, TEXTURE);
        addDrawable(frameCounterButton);
    }

    /**
     * When screen is opened - all keys are released. If we do not refresh them - player would stop moving (if they had).
     */
    private void refreshMovementKeys() {
        Consumer<KeyBinding> update = keyMapping -> {
            if (keyMapping.boundKey.getCategory() == InputUtil.Type.MOUSE) {
                keyMapping.setPressed(MouseHandler.isMouseButtonHeld(keyMapping.boundKey.getCode()));
            }
            else {
                long windowId = MinecraftClient.getInstance().getWindow().getHandle();
                keyMapping.setPressed(InputUtil.isKeyPressed(windowId, keyMapping.boundKey.getCode()));
            }
        };

        update.accept(ExposureClient.getViewfinderControlsKey());
        GameOptions opt = MinecraftClient.getInstance().options;
        update.accept(opt.forwardKey);
        update.accept(opt.backKey);
        update.accept(opt.leftKey);
        update.accept(opt.rightKey);
        update.accept(opt.jumpKey);
        update.accept(opt.sprintKey);
    }

    @Override
    public void render(@NotNull DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!ViewfinderClient.isLookingThrough()) {
            this.close();
            return;
        }

        if (MinecraftClient.getInstance().options.hudHidden)
            return;

        guiGraphics.getMatrices().push();

        float viewfinderScale = ViewfinderOverlay.getScale();
        if (viewfinderScale != 1.0f) {
            guiGraphics.getMatrices().translate(width / 2f, height / 2f, 0);
            guiGraphics.getMatrices().scale(viewfinderScale, viewfinderScale, viewfinderScale);
            guiGraphics.getMatrices().translate(-width / 2f, -height / 2f, 0);
        }

        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        for(Drawable drawable : this.drawables) {
            if (drawable instanceof IElementWithTooltip tooltipElement && drawable instanceof ClickableWidget widget
                && widget.visible && widget.isSelected()) {
                tooltipElement.renderToolTip(guiGraphics, mouseX, mouseY);
                break;
            }
        }

        guiGraphics.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        if (!handled && button == 1 && MinecraftClient.getInstance().interactionManager != null) {
            Hand activeHand = CameraInHand.getActiveHand(player);
            if (activeHand != null) {
                ItemStack itemInHand = player.getStackInHand(activeHand);
                if (itemInHand.getItem() instanceof CameraItem) {
                    MinecraftClient.getInstance().interactionManager.interactItem(player, activeHand);
                }
            }

            handled = true;
        }

        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (ExposureClient.getViewfinderControlsKey().matchesMouse(button)) {
            if (level.getTime() - openedAtTimestamp >= 5)
                this.close();

            return false;
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (ExposureClient.getViewfinderControlsKey().matchesKey(keyCode, scanCode)) {
            if (level.getTime() - openedAtTimestamp >= 5)
                this.close();

            return false;
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Preconditions.checkState(client != null);

        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        if (handled)
            return true;

        if (keyCode == InputUtil.GLFW_KEY_KP_ADD || keyCode == InputUtil.GLFW_KEY_EQUAL) {
            ViewfinderClient.zoom(ZoomDirection.IN, true);
            return true;
        }
        else if (keyCode == 333 /*KEY_SUBTRACT*/ || keyCode == InputUtil.GLFW_KEY_MINUS) {
            ViewfinderClient.zoom(ZoomDirection.OUT, true);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!super.mouseScrolled(mouseX, mouseY, delta)) {
            ViewfinderClient.zoom(delta > 0d ? ZoomDirection.IN : ZoomDirection.OUT, true);
            return true;
        }

        return false;
    }
}
