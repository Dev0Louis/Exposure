package io.github.mortuusars.exposure.command.exposure;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.client.LoadExposureCommandS2CP;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class LoadCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> get() {
        return CommandManager.literal("load")
                .then(CommandManager.literal("withDithering")
                        .then(CommandManager.argument("size", IntegerArgumentType.integer(1, 2048))
                                .then(CommandManager.argument("path", StringArgumentType.string())
                                        .then(CommandManager.argument("id", StringArgumentType.string())
                                                .executes(context -> loadExposureFromFile(context.getSource(),
                                                        StringArgumentType.getString(context, "id"),
                                                        StringArgumentType.getString(context, "path"),
                                                        IntegerArgumentType.getInteger(context, "size"), true))))))
                .then(CommandManager.argument("size", IntegerArgumentType.integer(1, 2048))
                        .then(CommandManager.argument("path", StringArgumentType.string())
                                .then(CommandManager.argument("id", StringArgumentType.string())
                                        .executes(context -> loadExposureFromFile(context.getSource(),
                                                StringArgumentType.getString(context, "id"),
                                                StringArgumentType.getString(context, "path"),
                                                IntegerArgumentType.getInteger(context, "size"), false)))));
    }

    private static int loadExposureFromFile(ServerCommandSource stack, String id, String path, int size, boolean dither) throws CommandSyntaxException {
        ServerPlayerEntity player = stack.getPlayerOrThrow();
        Packets.sendToClient(new LoadExposureCommandS2CP(id, path, size, dither), player);
        return 0;
    }
}
