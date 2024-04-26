package io.github.mortuusars.exposure.render;

import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.entity.PhotographEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.NotNull;

public class PhotographEntityRenderer<T extends PhotographEntity> extends EntityRenderer<T> {

    public PhotographEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public @NotNull Identifier getTexture(@NotNull T pEntity) {
        return PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;
    }

    @Override
    public boolean shouldRender(T livingEntity, Frustum camera, double camX, double camY, double camZ) {
        return super.shouldRender(livingEntity, camera, camX, camY, camZ);
    }

    @Override
    public void render(@NotNull T entity, float entityYaw, float partialTick, @NotNull MatrixStack poseStack, @NotNull VertexConsumerProvider bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        boolean invisible = entity.isInvisible();

        poseStack.push();

        poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getPitch()));
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entity.getYaw()));
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((entity.getRotation() * 360.0F / 4.0F)));
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));

        poseStack.translate(-0.5, -0.5, 1f / 32f - 0.005);
        float scale = 1f / ExposureClient.getExposureRenderer().getSize();
        poseStack.scale(scale, scale, -scale);

        int brightness = switch (entity.getHorizontalFacing()) {
            case DOWN -> 210;
            case UP -> 255;
            default -> 235;
        };

        if (entity.isGlowing())
            packedLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        ItemStack item = entity.getItem();

        PhotographRenderer.render(item, !invisible, true, poseStack, bufferSource, packedLight,
                brightness, brightness, brightness, 255);

        poseStack.pop();
    }
}
