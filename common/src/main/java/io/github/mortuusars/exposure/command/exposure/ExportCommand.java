package io.github.mortuusars.exposure.command.exposure;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.logging.LogUtils;
import io.github.mortuusars.exposure.ExposureServer;
import io.github.mortuusars.exposure.command.suggestion.ExposureIdSuggestionProvider;
import io.github.mortuusars.exposure.data.ExposureLook;
import io.github.mortuusars.exposure.command.argument.ExposureLookArgument;
import io.github.mortuusars.exposure.command.argument.ExposureSizeArgument;
import io.github.mortuusars.exposure.data.storage.ExposureExporter;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import io.github.mortuusars.exposure.data.ExposureSize;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.WorldSavePath;

import static net.minecraft.server.command.CommandManager.*;

public class ExportCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> get() {
        return literal("export")
                .requires((stack) -> stack.hasPermissionLevel(3))
                .then(id())
                .then(all());
    }

    private static ArgumentBuilder<ServerCommandSource, ?> id() {
        return literal("id")
                .then(argument("id", StringArgumentType.string())
                        .suggests(new ExposureIdSuggestionProvider())
                        .executes(context -> exportExposures(context.getSource(),
                                List.of(StringArgumentType.getString(context, "id")),
                                ExposureSize.X1,
                                ExposureLook.REGULAR))
                        .then(argument("size", new ExposureSizeArgument())
                                .executes(context -> exportExposures(context.getSource(),
                                        List.of(StringArgumentType.getString(context, "id")),
                                        ExposureSizeArgument.getSize(context, "size"),
                                        ExposureLook.REGULAR))
                                .then(argument("look", new ExposureLookArgument())
                                        .executes(context -> exportExposures(context.getSource(),
                                                List.of(StringArgumentType.getString(context, "id")),
                                                ExposureSizeArgument.getSize(context, "size"),
                                                ExposureLookArgument.getLook(context, "look"))))));
    }

    private static ArgumentBuilder<ServerCommandSource, ?> all() {
        return literal("all")
                .executes(context -> exportAll(context.getSource(), ExposureSize.X1, ExposureLook.REGULAR))
                .then(argument("size", new ExposureSizeArgument())
                        .executes(context -> exportAll(context.getSource(),
                                ExposureSizeArgument.getSize(context, "size"),
                                ExposureLook.REGULAR))
                        .then(argument("look", new ExposureLookArgument())
                                .executes(context -> exportAll(context.getSource(),
                                        ExposureSizeArgument.getSize(context, "size"),
                                        ExposureLookArgument.getLook(context, "look")))));
    }

    private static int exportAll(ServerCommandSource source, ExposureSize size, ExposureLook look) {
        List<String> ids = ExposureServer.getExposureStorage().getAllIds();
        return exportExposures(source, ids, size, look);
    }

    private static int exportExposures(ServerCommandSource stack, List<String> exposureIds, ExposureSize size, ExposureLook look) {
        int savedCount = 0;

        File folder = stack.getServer().getSavePath(WorldSavePath.ROOT).resolve("exposures").toFile();
        boolean ignored = folder.mkdirs();

        for (String id : exposureIds) {
            Optional<ExposureSavedData> data = ExposureServer.getExposureStorage().getOrQuery(id);
            if (data.isEmpty()) {
                stack.sendError(Text.translatable("command.exposure.export.failure.not_found", id));
                continue;
            }

            ExposureSavedData exposureSavedData = data.get();
            String name = id + look.getIdSuffix();

            boolean saved = new ExposureExporter(name)
                    .withFolder(folder.getAbsolutePath())
                    .withModifier(look.getModifier())
                    .withSize(size)
                    .save(exposureSavedData);

            if (saved)
                stack.sendFeedback(() ->
                        Text.translatable("command.exposure.export.success.saved_exposure_id", id), true);

            savedCount++;
        }

        if (savedCount > 0) {
            String folderPath = getFolderPath(folder);
            Text folderComponent = Text.literal(folderPath)
                    .formatted(Formatting.UNDERLINE)
                    .styled(arg -> arg.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, folderPath)));
            Text component = Text.translatable("command.exposure.export.success.result", savedCount, folderComponent);
            stack.sendFeedback(() -> component, true);
        } else
            stack.sendError(Text.translatable("command.exposure.export.failure.none_saved"));

        return 0;
    }

    @NotNull
    private static String getFolderPath(File folder) {
        String folderPath;
        try {
            folderPath = folder.getCanonicalPath();
        } catch (IOException e) {
            LogUtils.getLogger().error(e.toString());
            folderPath = folder.getAbsolutePath();
        }
        return folderPath;
    }
}
