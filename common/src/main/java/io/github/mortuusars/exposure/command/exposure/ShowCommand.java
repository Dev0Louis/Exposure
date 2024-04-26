package io.github.mortuusars.exposure.command.exposure;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.mortuusars.exposure.ExposureServer;
import io.github.mortuusars.exposure.command.argument.TextureLocationArgument;
import io.github.mortuusars.exposure.command.suggestion.ExposureIdSuggestionProvider;
import io.github.mortuusars.exposure.data.storage.ExposureSavedData;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.client.ShowExposureS2CP;
import java.util.Optional;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ShowCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> get() {
        return CommandManager.literal("show")
                .then(CommandManager.literal("latest")
                        .executes(context -> latest(context.getSource(), false))
                        .then(CommandManager.literal("negative")
                                .executes(context -> latest(context.getSource(), true))))
                .then(CommandManager.literal("id")
                        .then(CommandManager.argument("id", StringArgumentType.string())
                                .suggests(new ExposureIdSuggestionProvider())
                                .executes(context -> exposureId(context.getSource(),
                                        StringArgumentType.getString(context, "id"), false))
                                .then(CommandManager.literal("negative")
                                        .executes(context -> exposureId(context.getSource(),
                                                StringArgumentType.getString(context, "id"), true)))))
                .then(CommandManager.literal("texture")
                        .then(CommandManager.argument("path", new TextureLocationArgument())
                                .executes(context -> texture(context.getSource(),
                                        IdentifierArgumentType.getIdentifier(context, "path"), false))
                                .then(CommandManager.literal("negative")
                                        .executes(context -> texture(context.getSource(),
                                                IdentifierArgumentType.getIdentifier(context, "path"), true)))));
    }

    private static int latest(ServerCommandSource stack, boolean negative) {
        ServerPlayerEntity player = stack.getPlayer();
        if (player == null) {
            stack.sendError(Text.translatable("command.exposure.show.error.not_a_player"));
            return 1;
        }

        Packets.sendToClient(ShowExposureS2CP.latest(negative), player);
        return 0;
    }

    private static int exposureId(ServerCommandSource stack, String id, boolean negative) {
        ServerPlayerEntity player = stack.getPlayer();
        if (player == null) {
            stack.sendError(Text.translatable("command.exposure.show.error.not_a_player"));
            return 1;
        }

        Optional<ExposureSavedData> exposureData = ExposureServer.getExposureStorage().getOrQuery(id);
        if (exposureData.isEmpty()) {
            stack.sendError(Text.translatable("command.exposure.show.error.not_found", id));
            return 0;
        }

        ExposureServer.getExposureSender().sendTo(player, id, exposureData.get());

        Packets.sendToClient(ShowExposureS2CP.id(id, negative), player);

        return 0;
    }

    private static int texture(ServerCommandSource stack, Identifier path, boolean negative) {
        ServerPlayerEntity player = stack.getPlayer();
        if (player == null) {
            stack.sendError(Text.translatable("command.exposure.show.error.not_a_player"));
            return 1;
        }

        Packets.sendToClient(ShowExposureS2CP.texture(path.toString(), negative), player);

        return 0;
    }
}
