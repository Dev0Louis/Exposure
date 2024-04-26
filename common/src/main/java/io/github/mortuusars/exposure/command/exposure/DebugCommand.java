package io.github.mortuusars.exposure.command.exposure;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.client.ClearRenderingCacheS2CP;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class DebugCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> get() {
        return CommandManager.literal("debug")
                .then(CommandManager.literal("clearRenderingCache")
                        .executes(DebugCommand::clearRenderingCache));
    }

    private static int clearRenderingCache(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource stack = context.getSource();
        ServerPlayerEntity player = stack.getPlayerOrThrow();
        Packets.sendToClient(new ClearRenderingCacheS2CP(), player);
        return 0;
    }
}
