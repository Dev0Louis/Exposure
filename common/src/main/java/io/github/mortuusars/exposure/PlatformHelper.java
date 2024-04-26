package io.github.mortuusars.exposure;

import dev.architectury.injectables.annotations.ExpectPlatform;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlatformHelper {
    @ExpectPlatform
    public static boolean canShear(ItemStack stack) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void openMenu(ServerPlayerEntity serverPlayer, NamedScreenHandlerFactory menuProvider, Consumer<PacketByteBuf> extraDataWriter) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<String> getDefaultSpoutDevelopmentColorSequence() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static List<String> getDefaultSpoutDevelopmentBWSequence() {
        throw new AssertionError();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @ExpectPlatform
    public static boolean isModLoaded(String modId) {
        throw new AssertionError();
    }
}
