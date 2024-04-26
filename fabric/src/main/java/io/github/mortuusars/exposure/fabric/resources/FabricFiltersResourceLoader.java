package io.github.mortuusars.exposure.fabric.resources;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.data.FiltersResourceLoader;
import io.github.mortuusars.exposure.data.LensesDataLoader;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.util.Identifier;

public class FabricFiltersResourceLoader extends FiltersResourceLoader implements IdentifiableResourceReloadListener {
    public static final Identifier ID = Exposure.resource("filters_loader");
    @Override
    public Identifier getFabricId() {
        return ID;
    }
}
