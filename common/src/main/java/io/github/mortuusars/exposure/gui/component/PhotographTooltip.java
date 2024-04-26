package io.github.mortuusars.exposure.gui.component;

import com.google.common.base.Preconditions;
import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.item.StackedPhotographsItem;
import io.github.mortuusars.exposure.render.PhotographRenderer;
import io.github.mortuusars.exposure.render.modifiers.ExposurePixelModifiers;
import io.github.mortuusars.exposure.util.ItemAndStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.item.ItemStack;

public class PhotographTooltip implements TooltipComponent, TooltipData {
    public static final int SIZE = 72;
    private final List<ItemAndStack<PhotographItem>> photographs;

    public PhotographTooltip(ItemStack photographStack) {
        Preconditions.checkArgument(photographStack.getItem() instanceof PhotographItem,
                photographStack + " is not a PhotographItem.");

        this.photographs = List.of(new ItemAndStack<>(photographStack));
    }

    public PhotographTooltip(ItemAndStack<StackedPhotographsItem> stackedPhotographs) {
        this.photographs = stackedPhotographs.getItem().getPhotographs(stackedPhotographs.getStack());
    }

    @Override
    public int getWidth(@NotNull TextRenderer font) {
        return SIZE;
    }

    @Override
    public int getHeight() {
        return SIZE + 2; // 2px bottom margin
    }

    @Override
    public void drawItems(@NotNull TextRenderer font, int mouseX, int mouseY, DrawContext guiGraphics) {
        int photographsCount = photographs.size();
        int additionalPhotographs = Math.min(2, photographsCount - 1);

        guiGraphics.getMatrices().push();
        guiGraphics.getMatrices().translate(mouseX, mouseY, 5);
        float scale = SIZE / (float) ExposureClient.getExposureRenderer().getSize();
        float nextPhotographOffset = PhotographRenderer.getStackedPhotographOffset() / ExposureClient.getExposureRenderer().getSize();
        scale *= 1f - (additionalPhotographs * nextPhotographOffset);
        guiGraphics.getMatrices().scale(scale, scale, 1f);

        VertexConsumerProvider.Immediate bufferSource = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

        PhotographRenderer.renderStackedPhotographs(photographs, guiGraphics.getMatrices(), bufferSource,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, 255, 255, 255, 255);

        bufferSource.draw();

        guiGraphics.getMatrices().pop();

        // Stack count:
        if (photographsCount > 1) {
            guiGraphics.getMatrices().push();
            String count = Integer.toString(photographsCount);
            int fontWidth = MinecraftClient.getInstance().textRenderer.getWidth(count);
            float fontScale = 1.6f;
            guiGraphics.getMatrices().translate(
                    mouseX + ExposureClient.getExposureRenderer().getSize() * scale - 2 - fontWidth * fontScale,
                    mouseY + ExposureClient.getExposureRenderer().getSize() * scale - 2 - 8 * fontScale,
                    10);
            guiGraphics.getMatrices().scale(fontScale, fontScale, fontScale);
            guiGraphics.drawTextWithShadow(font, count, 0, 0, 0xFFFFFFFF);
            guiGraphics.getMatrices().pop();
        }
    }
}
