package io.github.mortuusars.exposure;

import io.github.mortuusars.exposure.item.CameraItem;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class CommonFunctionality {
    public static void handleItemDrop(@Nullable PlayerEntity player, @Nullable ItemEntity droppedItemEntity) {
        if (player == null || droppedItemEntity == null)
            return;

        ItemStack stack = droppedItemEntity.getStack();
        if (stack.getItem() instanceof CameraItem cameraItem && cameraItem.isActive(stack))
            cameraItem.deactivate(player, stack);
    }
}
