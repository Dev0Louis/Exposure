package io.github.mortuusars.exposure.render;

import io.github.mortuusars.exposure.ExposureClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public class PhotographInHandRenderer {
    public static void renderPhotograph(MatrixStack poseStack, VertexConsumerProvider bufferSource, int combinedLight, ItemStack stack) {
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        poseStack.scale(0.38f, 0.38f, 0.38f);
        poseStack.translate(-0.5, -0.5, 0);
        float scale = 1f / ExposureClient.getExposureRenderer().getSize();
        poseStack.scale(scale, scale, -scale);

        PhotographRenderer.render(stack, true, false, poseStack, bufferSource,
                combinedLight, 255, 255, 255, 255);
    }
}
