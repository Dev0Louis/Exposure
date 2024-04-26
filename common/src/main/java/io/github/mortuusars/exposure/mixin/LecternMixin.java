package io.github.mortuusars.exposure.mixin;

import io.github.mortuusars.exposure.PlatformHelper;
import io.github.mortuusars.exposure.item.AlbumItem;
import io.github.mortuusars.exposure.menu.LecternAlbumMenu;
import io.github.mortuusars.exposure.util.ItemAndStack;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LecternBlock.class)
public abstract class LecternMixin {
    @Inject(method = "openScreen", at = @At(value = "HEAD"), cancellable = true)
    private void openScreen(World level, BlockPos pos, PlayerEntity player, CallbackInfo ci) {
        if (level.getBlockEntity(pos) instanceof LecternBlockEntity lecternBlockEntity
                && player instanceof ServerPlayerEntity serverPlayer
                && lecternBlockEntity.getBook().getItem() instanceof AlbumItem) {
            exposure$open(serverPlayer, lecternBlockEntity, lecternBlockEntity.getBook());
            player.incrementStat(Stats.INTERACT_WITH_LECTERN);
            ci.cancel();
        }
    }

    @Unique
    private void exposure$open(ServerPlayerEntity player, LecternBlockEntity lecternBlockEntity, ItemStack albumStack) {
        NamedScreenHandlerFactory menuProvider = new NamedScreenHandlerFactory() {
            @Override
            public @NotNull Text getDisplayName() {
                return albumStack.getName();
            }

            @Override
            public @NotNull ScreenHandler createMenu(int containerId, @NotNull PlayerInventory playerInventory, @NotNull PlayerEntity player) {
                LecternBlockEntityAccessor accessor = (LecternBlockEntityAccessor) lecternBlockEntity;
                return new LecternAlbumMenu(containerId, lecternBlockEntity.getPos(), playerInventory,
                        new ItemAndStack<>(albumStack), accessor.getInventory(), accessor.getPropertyDelegate());
            }
        };

        PlatformHelper.openMenu(player, menuProvider, buffer -> {
            buffer.writeBlockPos(lecternBlockEntity.getPos());
            buffer.writeItemStack(albumStack);
        });
    }
}
