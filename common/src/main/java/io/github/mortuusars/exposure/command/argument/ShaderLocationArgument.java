package io.github.mortuusars.exposure.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.util.Identifier;

public class ShaderLocationArgument extends IdentifierArgumentType {
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestIdentifiers(getShaderLocations(), builder);
    }

    private static Stream<Identifier> getShaderLocations() {
        return MinecraftClient.getInstance().getResourceManager()
                .findResources("shaders", ShaderLocationArgument::filterLocations)
                .keySet()
                .stream();
    }

    private static boolean filterLocations(Identifier resourceLocation) {
        return resourceLocation.getPath().endsWith(".json")
                && !resourceLocation.getPath().contains("shaders/program")
                && !resourceLocation.getPath().contains("shaders/core");
    }
}
