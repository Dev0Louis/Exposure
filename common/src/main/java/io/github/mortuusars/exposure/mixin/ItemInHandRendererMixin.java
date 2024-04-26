package io.github.mortuusars.exposure.mixin;

import io.github.mortuusars.exposure.camera.viewfinder.ViewfinderClient;
import io.github.mortuusars.exposure.render.PhotographInHandRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.item.StackedPhotographsItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HeldItemRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow
    private ItemStack mainHand;
    @Shadow
    private ItemStack offHand;
    @Shadow
    protected abstract void renderArmHoldingItem(MatrixStack pMatrixStack, VertexConsumerProvider pBuffer, int pCombinedLight, float pEquippedProgress, float pSwingProgress, Arm pSide);
    @Shadow
    protected abstract float getMapAngle(float pPitch);
    @Shadow
    protected abstract void renderArm(MatrixStack pMatrixStack, VertexConsumerProvider pBuffer, int pCombinedLight, Arm pSide);

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 0),
            locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void renderPhotograph(AbstractClientPlayerEntity player, float partialTicks, float pitch, Hand hand,
                                  float swingProgress, ItemStack stack, float equipProgress, MatrixStack poseStack,
                                  VertexConsumerProvider buffer, int combinedLight, CallbackInfo ci, boolean isMainHand, Arm arm) {
        if (ViewfinderClient.isLookingThrough()) {
            poseStack.pop();
            ci.cancel();
            return;
        }

        if (stack.getItem() instanceof PhotographItem || stack.getItem() instanceof StackedPhotographsItem) {
            if (isMainHand && this.offHand.isEmpty()) {
                exposure$renderTwoHandedPhotograph(player, poseStack, buffer, combinedLight, pitch, equipProgress, swingProgress);
            } else {
                exposure$renderOneHandedPhotograph(player, poseStack, buffer, combinedLight, equipProgress, arm, swingProgress, stack);
            }

            poseStack.pop();

            ci.cancel();
        }
    }

    @Unique
    private void exposure$renderOneHandedPhotograph(AbstractClientPlayerEntity player, MatrixStack matrixStack, VertexConsumerProvider buffer, int combinedLight, float pEquippedProgress, Arm pHand, float pSwingProgress, ItemStack stack) {
        float f = pHand == Arm.RIGHT ? 1.0F : -1.0F;
        matrixStack.translate(f * 0.125F, -0.125D, 0.0D);
        if (!player.isInvisible()) {
            matrixStack.push();
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(f * 10.0F));
            this.renderArmHoldingItem(matrixStack, buffer, combinedLight, pEquippedProgress, pSwingProgress, pHand);
            matrixStack.pop();
        }

        matrixStack.push();
        matrixStack.translate(f * 0.51F, -0.08F + pEquippedProgress * -1.2F, -0.75D);
        float f1 = MathHelper.sqrt(pSwingProgress);
        float f2 = MathHelper.sin(f1 * (float)Math.PI);
        float f3 = -0.5F * f2;
        float f4 = 0.4F * MathHelper.sin(f1 * ((float)Math.PI * 2F));
        float f5 = -0.3F * MathHelper.sin(pSwingProgress * (float)Math.PI);
        matrixStack.translate(f * f3, f4 - 0.3F * f2, f5);
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f2 * -45.0F));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f * f2 * -30.0F));
        PhotographInHandRenderer.renderPhotograph(matrixStack, buffer, combinedLight, stack);
        matrixStack.pop();
    }

    @Unique
    private void exposure$renderTwoHandedPhotograph(AbstractClientPlayerEntity player, MatrixStack matrixStack, VertexConsumerProvider pBuffer, int pCombinedLight, float pPitch, float pEquippedProgress, float pSwingProgress) {
        float f = MathHelper.sqrt(pSwingProgress);
        float f1 = -0.2F * MathHelper.sin(pSwingProgress * (float)Math.PI);
        float f2 = -0.4F * MathHelper.sin(f * (float)Math.PI);
        matrixStack.translate(0.0D, -f1 / 2.0F, f2);
        float f3 = this.getMapAngle(pPitch);
        matrixStack.translate(0.0D, 0.04F + pEquippedProgress * -1.2F + f3 * -0.5F, -0.72F);
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f3 * -85.0F));
        if (!player.isInvisible()) {
            matrixStack.push();
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
            this.renderArm(matrixStack, pBuffer, pCombinedLight, Arm.RIGHT);
            this.renderArm(matrixStack, pBuffer, pCombinedLight, Arm.LEFT);
            matrixStack.pop();
        }

        float f4 = MathHelper.sin(f * (float)Math.PI);
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f4 * 20.0F));
        matrixStack.scale(2.0F, 2.0F, 2.0F);
        PhotographInHandRenderer.renderPhotograph(matrixStack, pBuffer, pCombinedLight, this.mainHand);
    }
}
