package io.github.mortuusars.exposure.item;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.CrossbowPosing;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;

public class CameraItemClientExtensions {
    public static void applyDefaultHoldingPose(BipedEntityModel<?> model, LivingEntity entity, Arm arm) {
        model.head.pitch += 0.4f; // If we turn head down completely - arms will be too low.
        if (arm == Arm.RIGHT) {
            CrossbowPosing.hold(model.rightArm, model.leftArm, model.head, true);
        } else if (arm == Arm.LEFT) {
            CrossbowPosing.hold(model.rightArm, model.leftArm, model.head, false);
        }
        model.head.pitch += 0.3f;
    }

    public static void applySelfieHoldingPose(BipedEntityModel<?> model, LivingEntity entity, Arm arm) {
        ModelPart cameraArm = arm == Arm.RIGHT ? model.rightArm : model.leftArm;

        // Arm follows camera:
        cameraArm.pitch = (-(float)Math.PI / 2F) + model.head.pitch + 0.15F;
        cameraArm.yaw = model.head.yaw + (arm == Arm.RIGHT ? -0.3f : 0.3f);
        cameraArm.roll = 0f;

        // Undo arm bobbing:
        CrossbowPosing.swingArm(cameraArm, entity.age + MinecraftClient.getInstance().getTickDelta(),
                arm == Arm.LEFT ? 1.0F : -1.0F);
    }

    public static float itemPropertyFunction(ItemStack stack, ClientWorld clientLevel, LivingEntity livingEntity, int seed) {
        if (stack.getItem() instanceof CameraItem cameraItem && cameraItem.isActive(stack)) {
            if (cameraItem.isInSelfieMode(stack))
                return livingEntity == MinecraftClient.getInstance().player ? 0.2f : 0.3f;
            else
                return 0.1f;
        }

        return 0f;
    }

    public static void releaseUseButton() {
        MinecraftClient.getInstance().options.useKey.setPressed(false);
    }
}
