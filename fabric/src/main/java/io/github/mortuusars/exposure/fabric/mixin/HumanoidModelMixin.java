package io.github.mortuusars.exposure.fabric.mixin;

import io.github.mortuusars.exposure.item.CameraItemClientExtensions;
import io.github.mortuusars.exposure.util.CameraInHand;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> extends AnimalModel<T> {
    @Shadow public abstract ModelPart getHead();
    @Final
    @Shadow public ModelPart hat;

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    void onSetupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity player))
            return;

        CameraInHand camera = CameraInHand.getActive(player);
        if (camera.isEmpty())
            return;

        Arm arm = MinecraftClient.getInstance().options.getMainArm().getValue();
        if (camera.getHand() == Hand.OFF_HAND)
            arm = arm.getOpposite();

        if (camera.getCamera().getItem().isInSelfieMode(camera.getStack()))
            CameraItemClientExtensions.applySelfieHoldingPose((BipedEntityModel<?>) (Object) this, entity, arm);
        else
            CameraItemClientExtensions.applyDefaultHoldingPose((BipedEntityModel<?>) (Object) this, entity, arm);

        hat.copyTransform(getHead());
    }
}
