package io.github.mortuusars.exposure.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class GuiUtil {
    public static void blit(MatrixStack poseStack, float minX, float maxX, float minY, float maxY, float blitOffset, float minU, float maxU, float minV, float maxV) {
        Matrix4f matrix = poseStack.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        bufferbuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        bufferbuilder.vertex(matrix, minX, maxY, blitOffset).texture(minU, maxV).next();
        bufferbuilder.vertex(matrix, maxX, maxY, blitOffset).texture(maxU, maxV).next();
        bufferbuilder.vertex(matrix, maxX, minY, blitOffset).texture(maxU, minV).next();
        bufferbuilder.vertex(matrix, minX, minY, blitOffset).texture(minU, minV).next();
        BufferRenderer.drawWithGlobalProgram(bufferbuilder.end());
    }

    public static void blit(MatrixStack poseStack, float x, float y, float width, float height, int u, int v, int textureWidth, int textureHeight, float blitOffset) {
        blit(poseStack, x, x + width, y, y + height, blitOffset,
                (u + 0.0F) / (float)textureWidth, (u + width) / (float)textureWidth, (v + 0.0F) / (float)textureHeight, (v + height) / (float)textureHeight);
    }
}
