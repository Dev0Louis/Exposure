package io.github.mortuusars.exposure.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.item.StackedPhotographsItem;
import io.github.mortuusars.exposure.util.ItemAndStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class PhotographRenderer {
    public static void render(ItemStack stack, boolean renderPaper, boolean renderBackside, MatrixStack poseStack,
                              VertexConsumerProvider bufferSource, int packedLight, int r, int g, int b, int a) {
        if (stack.getItem() instanceof PhotographItem photographItem)
            renderPhotograph(photographItem, stack, renderPaper, renderBackside, poseStack, bufferSource, packedLight, r, g, b, a);
        else if (stack.getItem() instanceof StackedPhotographsItem stackedPhotographsItem)
            renderStackedPhotographs(stackedPhotographsItem, stack, poseStack, bufferSource, packedLight, r, g, b, a);
    }

    public static void renderPhotograph(PhotographItem photographItem, ItemStack stack, boolean renderPaper,
                                        boolean renderBackside, MatrixStack poseStack, VertexConsumerProvider bufferSource,
                                        int packedLight, int r, int g, int b, int a) {
        PhotographRenderProperties properties = PhotographRenderProperties.get(stack);
        int size = ExposureClient.getExposureRenderer().getSize();

        if (renderPaper) {
            renderTexture(properties.getPaperTexture(), poseStack, bufferSource, 0, 0,
                    size, size, packedLight, r, g, b, a);

            if (renderBackside) {
                poseStack.push();
                poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                poseStack.translate(-size, 0, -0.5);

                renderTexture(properties.getPaperTexture(), poseStack, bufferSource,
                        packedLight, (int) (r * 0.85f), (int) (g * 0.85f), (int) (b * 0.85f), a);

                poseStack.pop();
            }
        }

        @Nullable Either<String, Identifier> idOrTexture = photographItem.getIdOrTexture(stack);

        if (idOrTexture != null) {
            if (renderPaper) {
                poseStack.push();
                float offset = size * 0.0625f;
                poseStack.translate(offset, offset, 1);
                poseStack.scale(0.875f, 0.875f, 0.875f);
                ExposureClient.getExposureRenderer().render(idOrTexture, properties.getModifier(), poseStack, bufferSource,
                        packedLight, r, g, b, a);
                poseStack.pop();
            } else {
                ExposureClient.getExposureRenderer().render(idOrTexture, properties.getModifier(), poseStack, bufferSource,
                        packedLight, r, g, b, a);
            }

            if (renderPaper && properties.hasPaperOverlayTexture()) {
                poseStack.push();
                poseStack.translate(0, 0, 2);
                renderTexture(properties.getPaperOverlayTexture(), poseStack, bufferSource, packedLight, r, g, b, a);
                poseStack.pop();
            }
        }
    }

    public static void renderStackedPhotographs(StackedPhotographsItem stackedPhotographsItem, ItemStack stack,
                                                MatrixStack poseStack, VertexConsumerProvider bufferSource,
                                                int packedLight, int r, int g, int b, int a) {
        List<ItemAndStack<PhotographItem>> photographs = stackedPhotographsItem.getPhotographs(stack, 3);
        renderStackedPhotographs(photographs, poseStack, bufferSource, packedLight, r, g, b, a);
    }

    public static void renderStackedPhotographs(List<ItemAndStack<PhotographItem>> photographs,
                                                MatrixStack poseStack, VertexConsumerProvider bufferSource,
                                                int packedLight, int r, int g, int b, int a) {
        if (photographs.isEmpty())
            return;

        for (int i = 2; i >= 0; i--) {
            if (photographs.size() - 1 < i)
                continue;

            ItemAndStack<PhotographItem> photograph = photographs.get(i);
            PhotographRenderProperties properties = PhotographRenderProperties.get(photograph.getStack());

            // Top photograph:
            if (i == 0) {
                poseStack.push();
                poseStack.translate(0, 0, 2);
                renderPhotograph(photograph.getItem(), photograph.getStack(), true, false, poseStack,
                        bufferSource, packedLight, r, g, b, a);
                poseStack.pop();
                break;
            }

            // Photographs below (only paper)
            float posOffset = getStackedPhotographOffset() * i;
            float rotateOffset = ExposureClient.getExposureRenderer().getSize() / 2f;

            poseStack.push();
            poseStack.translate(posOffset, posOffset, 2 - i);

            poseStack.translate(rotateOffset, rotateOffset, 0);
            poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 90 + 90));
            poseStack.translate(-rotateOffset, -rotateOffset, 0);

            float brightnessMul = 1f - (getStackedBrightnessStep() * i);

            renderTexture(properties.getPaperTexture(), poseStack, bufferSource,
                    packedLight, (int)(r * brightnessMul), (int)(g * brightnessMul), (int)(b * brightnessMul), a);

            poseStack.pop();
        }
    }

    public static float getStackedBrightnessStep() {
        return 0.15f;
    }

    public static float getStackedPhotographOffset() {
        // 2 px / Texture size (64px) = 0.03125
        return ExposureClient.getExposureRenderer().getSize() * 0.03125f;
    }

    private static void renderTexture(Identifier resource, MatrixStack poseStack, VertexConsumerProvider bufferSource,
                                      int packedLight, int r, int g, int b, int a) {
        renderTexture(resource, poseStack, bufferSource, 0, 0, ExposureClient.getExposureRenderer().getSize(),
                ExposureClient.getExposureRenderer().getSize(), packedLight, r, g, b, a);
    }

    private static void renderTexture(Identifier resource, MatrixStack poseStack, VertexConsumerProvider bufferSource,
                                      float x, float y, float width, float height, int packedLight, int r, int g, int b, int a) {
        renderTexture(resource, poseStack, bufferSource, x, y, x + width, y + height,
                0, 0, 1, 1, packedLight, r, g, b, a);
    }

    private static void renderTexture(Identifier resource, MatrixStack poseStack, VertexConsumerProvider bufferSource,
                                      float minX, float minY, float maxX, float maxY,
                                      float minU, float minV, float maxU, float maxV, int packedLight, int r, int g, int b, int a) {
        RenderSystem.setShaderTexture(0, resource);
        RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapProgram);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();

        Matrix4f matrix = poseStack.peek().getPositionMatrix();
        VertexConsumer bufferBuilder = bufferSource.getBuffer(RenderLayer.getText(resource));
        bufferBuilder.vertex(matrix, minX, maxY, 0).color(r, g, b, a).texture(minU, maxV).light(packedLight).next();
        bufferBuilder.vertex(matrix, maxX, maxY, 0).color(r, g, b, a).texture(maxU, maxV).light(packedLight).next();
        bufferBuilder.vertex(matrix, maxX, minY, 0).color(r, g, b, a).texture(maxU, minV).light(packedLight).next();
        bufferBuilder.vertex(matrix, minX, minY, 0).color(r, g, b, a).texture(minU, minV).light(packedLight).next();
    }
}