package io.github.mortuusars.exposure.data;

import io.github.mortuusars.exposure.render.modifiers.ExposurePixelModifiers;
import io.github.mortuusars.exposure.render.modifiers.IPixelModifier;
import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum ExposureLook implements StringIdentifiable {
    REGULAR("regular", ExposurePixelModifiers.EMPTY),
    AGED("aged", ExposurePixelModifiers.AGED),
    NEGATIVE("negative", ExposurePixelModifiers.NEGATIVE),
    NEGATIVE_FILM("negative_film", ExposurePixelModifiers.NEGATIVE_FILM);

    private final String name;
    private final IPixelModifier modifier;

    ExposureLook(String name, IPixelModifier modifier) {
        this.name = name;
        this.modifier = modifier;
    }

    public static @Nullable ExposureLook byName(String name) {
        for (ExposureLook value : values()) {
            if (value.asString().equals(name))
                return value;
        }

        return null;
    }

    @Override
    public @NotNull String asString() {
        return name;
    }

    public IPixelModifier getModifier() {
        return modifier;
    }

    public String getIdSuffix() {
        return this != REGULAR ? "_" + asString() : "";
    }
}