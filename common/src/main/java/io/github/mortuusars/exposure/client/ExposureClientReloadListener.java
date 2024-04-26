package io.github.mortuusars.exposure.client;

import io.github.mortuusars.exposure.ExposureClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.NotNull;

public class ExposureClientReloadListener extends SinglePreparationResourceReloader<Boolean> {
    @Override
    protected @NotNull Boolean prepare(ResourceManager resourceManager, Profiler profiler) {
        return true;
    }

    @Override
    protected void apply(Boolean object, ResourceManager resourceManager, Profiler profiler) {
        ExposureClient.getExposureStorage().clear();
        ExposureClient.getExposureRenderer().clearData();
    }
}
