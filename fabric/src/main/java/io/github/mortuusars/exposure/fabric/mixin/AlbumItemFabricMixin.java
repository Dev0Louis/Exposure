package io.github.mortuusars.exposure.fabric.mixin;

import io.github.mortuusars.exposure.item.AlbumItem;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AlbumItem.class, remap = false)
public abstract class AlbumItemFabricMixin implements FabricItem {
    @Shadow
    abstract boolean shouldPlayEquipAnimation(ItemStack oldStack, ItemStack newStack);
    @Override
    public boolean allowNbtUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        return shouldPlayEquipAnimation(oldStack, newStack);
    }
}
