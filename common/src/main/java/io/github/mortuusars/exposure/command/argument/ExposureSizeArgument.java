package io.github.mortuusars.exposure.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.mortuusars.exposure.data.ExposureSize;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

public class ExposureSizeArgument implements ArgumentType<ExposureSize> {
    @Override
    public ExposureSize parse(StringReader reader) throws CommandSyntaxException {
        String string = reader.readString();
        @Nullable ExposureSize size = ExposureSize.byName(string);

        if (size == null)
            throw new SimpleCommandExceptionType(Text.translatable("argument.enum.invalid", string)).create();

        return size;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(Arrays.stream(ExposureSize.values())
                .map(ExposureSize::asString), builder);
    }

    public static ExposureSize getSize(final CommandContext<?> context, final String name) {
        return context.getArgument(name, ExposureSize.class);
    }
}
