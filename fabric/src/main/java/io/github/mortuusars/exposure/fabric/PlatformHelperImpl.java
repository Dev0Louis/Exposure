package io.github.mortuusars.exposure.fabric;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class PlatformHelperImpl {
    public static boolean canShear(ItemStack stack) {
        return stack.getItem() instanceof ShearsItem;
    }

    public static void openMenu(ServerPlayerEntity serverPlayer, NamedScreenHandlerFactory menuProvider, Consumer<PacketByteBuf> extraDataWriter) {
        ExtendedScreenHandlerFactory extendedScreenHandlerFactory = new ExtendedScreenHandlerFactory() {
            @Nullable
            @Override
            public ScreenHandler createMenu(int i, @NotNull PlayerInventory inventory, @NotNull PlayerEntity player) {
                return menuProvider.createMenu(i, inventory, player);
            }

            @Override
            public @NotNull Text getDisplayName() {
                return menuProvider.getDisplayName();
            }

            @Override
            public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buffer) {
                extraDataWriter.accept(buffer);
            }
        };

        serverPlayer.openHandledScreen(extendedScreenHandlerFactory);
    }

    public static List<String> getDefaultSpoutDevelopmentColorSequence() {
        return List.of(
                "{FluidName:\"create:potion\",Amount:27000,Tag:{Potion:\"minecraft:awkward\"}}",
                "{FluidName:\"create:potion\",Amount:27000,Tag:{Potion:\"minecraft:thick\"}}",
                "{FluidName:\"create:potion\",Amount:27000,Tag:{Potion:\"minecraft:mundane\"}}");
    }

    public static List<String> getDefaultSpoutDevelopmentBWSequence() {
        return List.of(
                "{FluidName:\"minecraft:water\",Amount:27000}");
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
