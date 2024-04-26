package io.github.mortuusars.exposure;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.camera.viewfinder.ViewfinderClient;
import io.github.mortuusars.exposure.data.storage.ClientsideExposureStorage;
import io.github.mortuusars.exposure.data.storage.IClientsideExposureStorage;
import io.github.mortuusars.exposure.data.storage.IExposureStorage;
import io.github.mortuusars.exposure.data.transfer.ExposureReceiver;
import io.github.mortuusars.exposure.data.transfer.ExposureSender;
import io.github.mortuusars.exposure.data.transfer.IExposureReceiver;
import io.github.mortuusars.exposure.data.transfer.IExposureSender;
import io.github.mortuusars.exposure.gui.screen.camera.ViewfinderControlsScreen;
import io.github.mortuusars.exposure.item.AlbumItem;
import io.github.mortuusars.exposure.item.CameraItemClientExtensions;
import io.github.mortuusars.exposure.item.ChromaticSheetItem;
import io.github.mortuusars.exposure.item.StackedPhotographsItem;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.render.ExposureRenderer;
import io.github.mortuusars.exposure.util.CameraInHand;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

public class ExposureClient {
    private static final IClientsideExposureStorage exposureStorage = new ClientsideExposureStorage();
    private static final ExposureRenderer exposureRenderer = new ExposureRenderer();

    private static IExposureSender exposureSender;
    private static IExposureReceiver exposureReceiver;

    @Nullable
    private static KeyBinding openViewfinderControlsKey = null;

    public static void init() {
        exposureSender = new ExposureSender((packet, player) -> Packets.sendToServer(packet));
        exposureReceiver = new ExposureReceiver(exposureStorage);

        ModelPredicateProviderRegistry.register(Exposure.Items.CAMERA.get(), new Identifier("camera_state"), CameraItemClientExtensions::itemPropertyFunction);
        ModelPredicateProviderRegistry.register(Exposure.Items.CHROMATIC_SHEET.get(), new Identifier("channels"), (stack, clientLevel, livingEntity, seed) ->
                stack.getItem() instanceof ChromaticSheetItem chromaticSheet ?
                        chromaticSheet.getExposures(stack).size() / 10f : 0f);
        ModelPredicateProviderRegistry.register(Exposure.Items.STACKED_PHOTOGRAPHS.get(), new Identifier("count"),
                (stack, clientLevel, livingEntity, seed) ->
                        stack.getItem() instanceof StackedPhotographsItem stackedPhotographsItem ?
                                stackedPhotographsItem.getPhotographsCount(stack) / 100f : 0f);
        ModelPredicateProviderRegistry.register(Exposure.Items.ALBUM.get(), new Identifier("photos"),
                (stack, clientLevel, livingEntity, seed) ->
                        stack.getItem() instanceof AlbumItem albumItem ? albumItem.getPhotographsCount(stack) / 100f : 0f);
    }

    public static IClientsideExposureStorage getExposureStorage() {
        return exposureStorage;
    }

    public static IExposureSender getExposureSender() {
        return exposureSender;
    }

    public static IExposureReceiver getExposureReceiver() {
        return exposureReceiver;
    }

    public static ExposureRenderer getExposureRenderer() {
        return exposureRenderer;
    }

    public static void registerKeymappings(Function<KeyBinding, KeyBinding> registerFunction) {
        KeyBinding keyMapping = new KeyBinding("key.exposure.camera_controls",
                InputUtil.UNKNOWN_KEY.getCode(), "category.exposure");

        openViewfinderControlsKey = registerFunction.apply(keyMapping);
    }

    public static void onScreenAdded(Screen screen) {
        if (ViewfinderClient.isOpen() && !(screen instanceof ViewfinderControlsScreen)) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null)
                CameraInHand.deactivate(player);
        }
    }

    public static KeyBinding getViewfinderControlsKey() {
        Preconditions.checkState(openViewfinderControlsKey != null,
                "Viewfinder Controls key mapping was not registered");

        return openViewfinderControlsKey.isUnbound() ? MinecraftClient.getInstance().options.sneakKey : openViewfinderControlsKey;
    }
}
