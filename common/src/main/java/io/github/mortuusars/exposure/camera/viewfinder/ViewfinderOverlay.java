package io.github.mortuusars.exposure.camera.viewfinder;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.mortuusars.exposure.Config;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.gui.screen.camera.ViewfinderControlsScreen;
import io.github.mortuusars.exposure.item.FilmRollItem;
import io.github.mortuusars.exposure.util.CameraInHand;
import io.github.mortuusars.exposure.util.GuiUtil;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.awt.geom.Rectangle2D;
import java.util.Optional;

public class ViewfinderOverlay {
    private static final Identifier VIEWFINDER_TEXTURE = Exposure.id("textures/gui/viewfinder/viewfinder.png");
    public static Rectangle2D.Float opening = new Rectangle2D.Float(0, 0, 0, 0);

    private static final MatrixStack POSE_STACK = new MatrixStack();
    private static MinecraftClient minecraft = MinecraftClient.getInstance();
    private static PlayerEntity player = minecraft.player;

    private static int backgroundColor;

    private static float scale = 1f;

    private static Float xRot = null;
    private static Float yRot = null;
    private static Float xRot0 = null;
    private static Float yRot0 = null;

    public static void setup() {
        minecraft = MinecraftClient.getInstance();
        player = minecraft.player;

        backgroundColor = Config.Client.getBackgroundColor();
        scale = 0.5f;
    }

    public static float getScale() {
        return scale;
    }

    public static void render() {
        final int width = minecraft.getWindow().getScaledWidth();
        final int height = minecraft.getWindow().getScaledHeight();

        scale = MathHelper.lerp(Math.min(0.5f * minecraft.getLastFrameDuration(), 0.5f), scale, 1f);
        float openingSize = Math.min(width, height);

        opening = new Rectangle2D.Float((width - openingSize) / 2f, (height - openingSize) / 2f, openingSize, openingSize);

        if (minecraft.options.hudHidden)
            return;

        CameraInHand camera = CameraInHand.getActive(player);
        if (camera.isEmpty())
            return;

        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        if (xRot == null || yRot == null || xRot0 == null || yRot0 == null) {
            xRot = player.getPitch();
            yRot = player.getYaw();
            xRot0 = xRot;
            yRot0 = yRot;
        }
        float delta = Math.min(0.75f * minecraft.getLastFrameDuration(), 0.75f);
        xRot0 = MathHelper.lerp(delta, xRot0, xRot);
        yRot0 = MathHelper.lerp(delta, yRot0, yRot);
        xRot = player.getPitch();
        yRot = player.getYaw();
        float xDelay = xRot - xRot0;
        float yDelay = yRot - yRot0;

        MatrixStack poseStack = POSE_STACK;
        poseStack.push();
        poseStack.translate(width / 2f, height / 2f, 0);
        poseStack.scale(scale, scale, scale);

        float attackAnim = player.getHandSwingProgress(minecraft.getTickDelta());
        if (attackAnim > 0.5f)
            attackAnim = 1f - attackAnim;
        poseStack.scale(1f - attackAnim * 0.4f, 1f - attackAnim * 0.6f, 1f - attackAnim * 0.4f);
        poseStack.translate(width / 16f * attackAnim, width / 5f * attackAnim, 0);
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(attackAnim, 0, 10)));
        poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.lerp(attackAnim, 0, 100)));

        poseStack.translate(-width / 2f - yDelay, -height / 2f - xDelay, 0);

        if (minecraft.options.getBobView().getValue())
            bobView(poseStack, minecraft.getTickDelta());

        // -9999 to cover all screen when poseStack is scaled down.
        // Left
        drawRect(poseStack, -9999, opening.y, opening.x, opening.y + opening.height, backgroundColor);
        // Right
        drawRect(poseStack, opening.x + opening.width, opening.y, width + 9999, opening.y + opening.height, backgroundColor);
        // Top
        drawRect(poseStack, -9999, -9999, width + 9999, opening.y, backgroundColor);
        // Bottom
        drawRect(poseStack, -9999, opening.y + opening.height, width + 9999, height + 9999, backgroundColor);

        // Shutter
        if (camera.getItem().isShutterOpen(camera.getStack()))
            drawRect(poseStack, opening.x, opening.y, opening.x + opening.width, opening.y + opening.height, 0xfa1f1d1b);

        // Opening Texture
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, VIEWFINDER_TEXTURE);
        GuiUtil.blit(poseStack, opening.x, opening.x + opening.width, opening.y, opening.y + opening.height, 0f, 0f, 1f, 0f, 1f);

        // Guide
        RenderSystem.setShaderTexture(0, Exposure.id("textures/gui/viewfinder/composition_guide/" +
                camera.getItem().getCompositionGuide(camera.getStack()).getId() + ".png"));
        GuiUtil.blit(poseStack, opening.x, opening.x + opening.width, opening.y, opening.y + opening.height, -1f, 0f, 1f, 0f, 1f);

        // Icons
        if (!(minecraft.currentScreen instanceof ViewfinderControlsScreen)) {
            Optional<ItemAndStack<FilmRollItem>> film = camera.getItem().getFilm(camera.getStack());
            if (film.isEmpty() || !film.get().getItem().canAddFrame(film.get().getStack())) {
                RenderSystem.setShaderTexture(0, Exposure.id("textures/gui/viewfinder/icon/no_film.png"));
                float cropFactor = Exposure.CROP_FACTOR;
                float fromEdge = (opening.height - (opening.height / (cropFactor))) / 2f;
                GuiUtil.blit(poseStack, (opening.x + (opening.width / 2) - 12), (opening.y + opening.height - ((fromEdge / 2 + 10))),
                        24, 19, 0, 0, 24, 19, 0);
            }
        }

        poseStack.pop();
    }

    public static void drawRect(MatrixStack poseStack, float minX, float minY, float maxX, float maxY, int color) {
        if (minX < maxX) {
            float temp = minX;
            minX = maxX;
            maxX = temp;
        }

        if (minY < maxY) {
            float temp = minY;
            minY = maxY;
            maxY = temp;
        }

        float alpha = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;

        Matrix4f matrix = poseStack.peek().getPositionMatrix();

        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        bufferbuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        bufferbuilder.vertex(matrix, minX, maxY, 0.0F).color(r, g, b, alpha).next();
        bufferbuilder.vertex(matrix, maxX, maxY, 0.0F).color(r, g, b, alpha).next();
        bufferbuilder.vertex(matrix, maxX, minY, 0.0F).color(r, g, b, alpha).next();
        bufferbuilder.vertex(matrix, minX, minY, 0.0F).color(r, g, b, alpha).next();
        BufferRenderer.drawWithGlobalProgram(bufferbuilder.end());
        RenderSystem.disableBlend();
    }

    public static void bobView(MatrixStack poseStack, float partialTicks) {
        if (minecraft.getCameraEntity() instanceof PlayerEntity pl) {
            float f = pl.horizontalSpeed - pl.prevHorizontalSpeed;
            float f1 = -(pl.horizontalSpeed + f * partialTicks);
            float f2 = MathHelper.lerp(partialTicks, pl.prevStrideDistance, pl.strideDistance);
            poseStack.translate((MathHelper.sin(f1 * (float) Math.PI) * f2 * 16F), (-Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2 * 32F)), 0.0D);
            poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(f1 * (float) Math.PI) * f2 * 3.0F));
        }
    }
}

