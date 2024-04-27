package io.github.mortuusars.exposure.fabric.resources;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.client.ExposureClientReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.util.Identifier;

public class ExposureFabricClientReloadListener extends ExposureClientReloadListener implements IdentifiableResourceReloadListener {
    public static final Identifier ID = Exposure.id("clear_client_exposures_cache");
    @Override
    public Identifier getFabricId() {
        return ID;
    }
}
