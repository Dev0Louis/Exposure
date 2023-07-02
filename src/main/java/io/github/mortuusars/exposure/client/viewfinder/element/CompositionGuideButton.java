package io.github.mortuusars.exposure.client.viewfinder.element;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.mortuusars.exposure.camera.Camera;
import io.github.mortuusars.exposure.camera.component.CompositionGuide;
import io.github.mortuusars.exposure.camera.component.CompositionGuides;
import io.github.mortuusars.exposure.client.viewfinder.ViewfinderControlsScreen;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class CompositionGuideButton extends ImageButton {
    private final Screen screen;
    private final ResourceLocation texture;
    private final ItemAndStack<CameraItem> camera;
    private final List<CompositionGuide> guides;
    private int currentGuideIndex = 0;

    private long lastChangeTime;

    public CompositionGuideButton(Screen screen, int x, int y, int width, int height, ResourceLocation texture, ItemAndStack<CameraItem> camera) {
        super(x, y, width, height, 118, 0, height, texture, 256, 256, button -> {}, Button.NO_TOOLTIP, Component.empty());
        this.screen = screen;
        this.texture = texture;
        this.camera = camera;
        guides = CompositionGuides.getGuides();

        CompositionGuide guide = camera.getItem().getCompositionGuide(camera.getStack());

        for (int i = 0; i < guides.size(); i++) {
            if (guides.get(i).getId().equals(guide.getId())) {
                currentGuideIndex = i;
                break;
            }
        }
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderButton(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        int offset = this.getYImage(this.isHoveredOrFocused());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        // Button
        blit(poseStack, x, y, 118, height  * (offset - 1), width, height);
        // Icon
        blit(poseStack, x + 3, y + 1, 15, 100 + currentGuideIndex * 13, 15, 13);

        this.renderBg(poseStack, minecraft, mouseX, mouseY);

//        if (this.isHoveredOrFocused())
//            this.renderToolTip(poseStack, mouseX, mouseY);
    }

    @Override
    public void renderToolTip(PoseStack poseStack, int mouseX, int mouseY) {
        screen.renderTooltip(poseStack, List.of(Component.translatable("gui.exposure.viewfinder.composition_guide.tooltip"),
                ((MutableComponent) getMessage()).withStyle(ChatFormatting.GRAY)), Optional.empty(), mouseX, mouseY);
    }

    @Override
    public @NotNull Component getMessage() {
        return guides.get(currentGuideIndex).translate();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered) {
            cycleGuide(button == 1);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        cycleGuide(delta < 0d);
        return true;
    }

    private void cycleGuide(boolean reverse) {
        if (System.currentTimeMillis() - lastChangeTime < 60)
            return;

        currentGuideIndex += reverse ? -1 : 1;
        if (currentGuideIndex < 0)
            currentGuideIndex = guides.size() - 1;
        else if (currentGuideIndex >= guides.size())
            currentGuideIndex = 0;

        camera.getItem().setCompositionGuide(camera.getStack(), guides.get(currentGuideIndex));
        Camera.updateAndSyncCameraInHand(camera.getStack());

        assert Minecraft.getInstance().player != null;
        Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK);
        lastChangeTime = System.currentTimeMillis();
    }
}
