package io.github.mortuusars.exposure.util;

import net.minecraft.util.StringIdentifiable;
import org.jetbrains.annotations.NotNull;

public enum Side implements StringIdentifiable {
    LEFT(0, "left"),
    RIGHT(1, "right");

    private final int index;
    private final String name;

    Side(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public @NotNull String asString() {
        return name;
    }
}
