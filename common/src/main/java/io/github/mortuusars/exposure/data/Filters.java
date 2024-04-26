package io.github.mortuusars.exposure.data;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.util.Identifier;

public class Filters {
    private static ConcurrentMap<Ingredient, Identifier> filters = new ConcurrentHashMap<>();

    public static void reload(ConcurrentMap<Ingredient, Identifier> newFilters) {
        filters.clear();
        filters = newFilters;
    }

    public static Optional<Identifier> getShaderOf(ItemStack stack) {
        for (var filter : filters.entrySet()) {
            if (filter.getKey().test(stack))
                return Optional.of(filter.getValue());
        }

        return Optional.empty();
    }
}
