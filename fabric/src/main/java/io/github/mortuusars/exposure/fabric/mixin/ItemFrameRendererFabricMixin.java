package io.github.mortuusars.exposure.fabric.mixin;

import io.github.mortuusars.exposure.render.ItemFramePhotographRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ItemFrameEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.ItemFrameEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameEntityRenderer.class)
public abstract class ItemFrameRendererFabricMixin<T extends ItemFrameEntity>
        extends EntityRenderer<T> {
    protected ItemFrameRendererFabricMixin(EntityRendererFactory.Context context) {
        super(context);
    }

    @Inject(method = "render(Lnet/minecraft/entity/decoration/ItemFrameEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/ItemRenderer;renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;IILnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;I)V"))
    void onItemFrameRender(T entity, float entityYaw, float partialTicks, MatrixStack poseStack, VertexConsumerProvider buffer, int packedLight, CallbackInfo ci) {
        poseStack.scale(2F, 2F, 2F);
        boolean rendered = ItemFramePhotographRenderer.render(entity, poseStack, buffer, packedLight);

        if (rendered) {
            poseStack.pop();
            ci.cancel();
        }
        else {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }
}
