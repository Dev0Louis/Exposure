package io.github.mortuusars.exposure.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mortuusars.exposure.command.argument.ShaderLocationArgument;
import io.github.mortuusars.exposure.network.Packets;
import io.github.mortuusars.exposure.network.packet.client.ApplyShaderS2CP;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ShaderCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("shader")
                .requires((stack) -> stack.hasPermissionLevel(2))
                .then(CommandManager.literal("apply")
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                                .then(CommandManager.argument("shaderLocation", new ShaderLocationArgument())
                                        .executes(ShaderCommand::applyShader))))
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                                .executes(ShaderCommand::removeShader))));
    }

    private static int applyShader(CommandContext<ServerCommandSource> context) {
        Identifier shaderLocation = IdentifierArgumentType.getIdentifier(context, "shaderLocation");
        for (ServerPlayerEntity targetPlayer : getTargetPlayers(context)) {
            Packets.sendToClient(new ApplyShaderS2CP(shaderLocation), targetPlayer);
        }
        return 0;
    }

    private static int removeShader(CommandContext<ServerCommandSource> context) {
        Identifier shaderLocation = new Identifier("minecraft:none");
        for (ServerPlayerEntity targetPlayer : getTargetPlayers(context)) {
            Packets.sendToClient(new ApplyShaderS2CP(shaderLocation), targetPlayer);
            context.getSource().sendFeedback(() -> Text.translatable("command.exposure.shader.removed"), false);
        }
        return 0;
    }

    private static List<ServerPlayerEntity> getTargetPlayers(CommandContext<ServerCommandSource> context) {
        try {
            return new ArrayList<>(EntityArgumentType.getPlayers(context, "targets"));
        } catch (CommandSyntaxException e) {
            return Collections.emptyList();
        }
    }
}
