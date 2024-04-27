package io.github.mortuusars.exposure.fabric.resources;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.data.LensesDataLoader;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.util.Identifier;

public class FabricLensesDataLoader extends LensesDataLoader implements IdentifiableResourceReloadListener {
    public static final Identifier ID = Exposure.id("lenses_data");
    @Override
    public Identifier getFabricId() {
        return ID;
    }
}
