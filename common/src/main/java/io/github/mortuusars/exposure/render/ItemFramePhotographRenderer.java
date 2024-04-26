package io.github.mortuusars.exposure.render;

import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.render.modifiers.ExposurePixelModifiers;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;

public class ItemFramePhotographRenderer {
    public static boolean render(ItemFrameEntity itemFrameEntity, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        ItemStack itemStack = itemFrameEntity.getHeldItemStack();
        if (!(itemStack.getItem() instanceof PhotographItem photographItem))
            return false;

        @Nullable Either<String, Identifier> idOrTexture = photographItem.getIdOrTexture(itemStack);
        if (idOrTexture == null)
            return false;

        if (itemFrameEntity.getType() == EntityType.GLOW_ITEM_FRAME)
            packedLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        poseStack.push();

        String entityName = Registries.ENTITY_TYPE.getId(itemFrameEntity.getType()).toString();
        if (entityName.equals("quark:glass_frame")) {
            poseStack.translate(0, 0, 0.475f);
        }

        // Snap to 90 degrees like a map.
        poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45 * itemFrameEntity.getRotation()));

        float size = ExposureClient.getExposureRenderer().getSize();

        float scale = 1f / size;
        float pixelScale = scale / 16f;
        scale -= pixelScale * 6;

        poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-size / 2f, -size / 2f, 10);

        PhotographRenderer.renderPhotograph(photographItem, itemStack, false, false,
                poseStack, bufferSource, packedLight, 255, 255, 255, 255);

        poseStack.pop();

        return true;
    }
}
