package io.github.mortuusars.exposure.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.mortuusars.exposure.command.exposure.*;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ExposureCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("exposure")
                .requires((stack) -> stack.hasPermissionLevel(2))
                .then(LoadCommand.get())
                .then(ExposeCommand.get())
                .then(ExportCommand.get())
                .then(ShowCommand.get())
                .then(DebugCommand.get()));
    }
}
