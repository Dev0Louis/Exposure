package io.github.mortuusars.exposure.mixin;

import io.github.mortuusars.exposure.Exposure;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @ModifyVariable(method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V", at = @At("HEAD"), argsOnly = true)
    BakedModel renderItem(BakedModel model, ItemStack itemStack, ModelTransformationMode displayContext, boolean leftHand,
                          MatrixStack poseStack, VertexConsumerProvider buffer, int combinedLight, int combinedOverlay) {
        if (MinecraftClient.getInstance().world != null && itemStack.isOf(Exposure.Items.CAMERA.get()) && displayContext == ModelTransformationMode.GUI) {
            BakedModel guiModel = MinecraftClient.getInstance().getBakedModelManager()
                    .getModel(new ModelIdentifier("exposure", "camera_gui", "inventory"));

            return guiModel.getOverrides().apply(guiModel, itemStack, MinecraftClient.getInstance().world, null, 0);
        }
        return model;
    }
}
