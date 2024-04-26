package io.github.mortuusars.exposure.gui.screen.album;

import com.google.common.collect.Lists;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.render.PhotographRenderProperties;
import io.github.mortuusars.exposure.render.PhotographRenderer;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.input.KeyCodes;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PhotographSlotButton extends TexturedButtonWidget {

    protected final Rect2i exposureArea;
    protected final PressAction onRightButtonPress;
    protected final Supplier<ItemStack> photograph;
    protected final boolean isEditable;
    protected boolean hasPhotograph;

    public PhotographSlotButton(Rect2i exposureArea, int x, int y, int width, int height, int xTexStart, int yTexStart,
                                int yDiffTex, Identifier resourceLocation, int textureWidth, int textureHeight,
                                PressAction onLeftButtonPress, PressAction onRightButtonPress, Supplier<ItemStack> photographGetter, boolean isEditable) {
        super(x, y, width, height, xTexStart, yTexStart, yDiffTex, resourceLocation, textureWidth, textureHeight, onLeftButtonPress,
                Text.translatable("item.exposure.photograph"));
        this.exposureArea = exposureArea;
        this.onRightButtonPress = onRightButtonPress;
        this.photograph = photographGetter;
        this.isEditable = isEditable;
    }

    public ItemStack getPhotograph() {
        return photograph.get();
    }

    @Override
    public void renderButton(DrawContext guiGraphics, int mouseX, int mouseY, float partialTick) {
        ItemStack photograph = getPhotograph();

        if (photograph.getItem() instanceof PhotographItem) {
            hasPhotograph = true;

            PhotographRenderProperties renderProperties = PhotographRenderProperties.get(photograph);

            // Paper
            drawTexture(guiGraphics, renderProperties.getAlbumPaperTexture(),
                    getX(), getY(), 0, 0, 0, width, height, width, height);

            // Exposure
            guiGraphics.getMatrices().push();
            float scale = exposureArea.getWidth() / (float) ExposureClient.getExposureRenderer().getSize();
            guiGraphics.getMatrices().translate(exposureArea.getX(), exposureArea.getY(), 1);
            guiGraphics.getMatrices().scale(scale, scale, scale);
            VertexConsumerProvider.Immediate bufferSource = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
            PhotographRenderer.render(photograph, false, false, guiGraphics.getMatrices(),
                    bufferSource, LightmapTextureManager.MAX_LIGHT_COORDINATE, 255, 255, 255, 255);
            bufferSource.draw();
            guiGraphics.getMatrices().pop();

            // Paper overlay
            if (renderProperties.hasAlbumPaperOverlayTexture()) {
                guiGraphics.getMatrices().push();
                guiGraphics.getMatrices().translate(0, 0, 2);
                drawTexture(guiGraphics, renderProperties.getAlbumPaperOverlayTexture(),
                        getX(), getY(), 0, 0, 0, width, height, width, height);
                guiGraphics.getMatrices().pop();
            }
        }
        else {
            hasPhotograph = false;
        }

        // Album pins
        int xTex = u + (hasPhotograph ? getWidth() : 0);
        drawTexture(guiGraphics, texture, getX(), getY(), xTex, v, hoveredVOffset, width, height, textureWidth, textureHeight);
    }

    public void renderTooltip(DrawContext guiGraphics, int mouseX, int mouseY) {
        if (isEditable && !hasPhotograph) {
            guiGraphics.drawTooltip(MinecraftClient.getInstance().textRenderer,
                    Text.translatable("gui.exposure.album.add_photograph"), mouseX, mouseY);
            return;
        }

        ItemStack photograph = getPhotograph();
        if (photograph.isEmpty())
            return;

        List<Text> itemTooltip = Screen.getTooltipFromItem(MinecraftClient.getInstance(), photograph);
        itemTooltip.add(Text.translatable("gui.exposure.album.left_click_or_scroll_up_to_view"));
        if (isEditable)
            itemTooltip.add(Text.translatable("gui.exposure.album.right_click_to_remove"));

        // Photograph image in tooltip is not rendered here

        if (isFocused()) {
            guiGraphics.drawTooltip(MinecraftClient.getInstance().textRenderer, Lists.transform(itemTooltip,
                    Text::asOrderedText), getTooltipPositioner(), mouseX, mouseY);
        }
        else
            guiGraphics.drawTooltip(MinecraftClient.getInstance().textRenderer, itemTooltip, Optional.empty(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || !clicked(mouseX, mouseY))
            return false;

        if (button == InputUtil.field_32000) {
            this.onPress.onPress(this);
        } else if (button == InputUtil.field_32002) {
            this.onRightButtonPress.onPress(this);
        } else
            return false;

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0 && clicked(mouseX, mouseY) && hasPhotograph) {
            this.onPress.onPress(this);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.active && this.visible && Screen.hasShiftDown() && KeyCodes.isToggle(keyCode)) {
            onRightButtonPress.onPress(this);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
