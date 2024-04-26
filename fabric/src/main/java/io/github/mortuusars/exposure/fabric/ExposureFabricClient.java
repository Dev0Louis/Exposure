package io.github.mortuusars.exposure.fabric;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.fabric.resources.ExposureFabricClientReloadListener;
import io.github.mortuusars.exposure.fabric.resources.FabricFiltersResourceLoader;
import io.github.mortuusars.exposure.fabric.resources.FabricLensesDataLoader;
import io.github.mortuusars.exposure.gui.component.PhotographTooltip;
import io.github.mortuusars.exposure.gui.screen.LightroomScreen;
import io.github.mortuusars.exposure.gui.screen.album.AlbumScreen;
import io.github.mortuusars.exposure.gui.screen.album.LecternAlbumScreen;
import io.github.mortuusars.exposure.gui.screen.camera.CameraAttachmentsScreen;
import io.github.mortuusars.exposure.network.fabric.PacketsImpl;
import io.github.mortuusars.exposure.render.PhotographEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.resource.ResourceType;

public class ExposureFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ExposureClient.init();

        ExposureClient.registerKeymappings(KeyBindingHelper::registerKeyBinding);

        HandledScreens.register(Exposure.MenuTypes.CAMERA.get(), CameraAttachmentsScreen::new);
        HandledScreens.register(Exposure.MenuTypes.ALBUM.get(), AlbumScreen::new);
        HandledScreens.register(Exposure.MenuTypes.LECTERN_ALBUM.get(), LecternAlbumScreen::new);
        HandledScreens.register(Exposure.MenuTypes.LIGHTROOM.get(), LightroomScreen::new);

        ModelLoadingPlugin.register(pluginContext ->
                pluginContext.addModels(new ModelIdentifier("exposure", "camera_gui", "inventory")));

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new ExposureFabricClientReloadListener());
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new FabricFiltersResourceLoader());

        EntityRendererRegistry.register(Exposure.EntityTypes.PHOTOGRAPH.get(), PhotographEntityRenderer::new);
        TooltipComponentCallback.EVENT.register(data -> data instanceof PhotographTooltip photographTooltip ? photographTooltip : null);

        PacketsImpl.registerS2CPackets();
    }
}
