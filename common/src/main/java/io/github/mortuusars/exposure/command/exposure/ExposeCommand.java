package io.github.mortuusars.exposure.command.exposure;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.client.ExposeCommandS2CP;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class ExposeCommand {
    public static LiteralArgumentBuilder<ServerCommandSource> get() {
        return CommandManager.literal("expose")
                .executes(context -> expose(context.getSource(), Integer.MAX_VALUE))
                .then(CommandManager.argument("size", IntegerArgumentType.integer(1, 2048))
                        .executes(context -> expose(context.getSource(), IntegerArgumentType.getInteger(context, "size"))));
    }

    private static int expose(ServerCommandSource stack, int size) throws CommandSyntaxException {
        ServerPlayerEntity player = stack.getPlayerOrThrow();
        Packets.sendToClient(new ExposeCommandS2CP(size), player);
        return 0;
    }
}
