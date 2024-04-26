package io.github.mortuusars.exposure.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.mortuusars.exposure.test.Tests;
import io.github.mortuusars.exposure.test.framework.TestResult;
import io.github.mortuusars.exposure.test.framework.TestingResult;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TestCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("test")
                        .requires((commandSourceStack -> commandSourceStack.hasPermissionLevel(3)))
                        .then(CommandManager.literal("exposure")
                                .executes(TestCommand::run)));
    }

    private static int run(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            TestingResult testingResult = new Tests(player).run();

            MutableText message = Text.literal("Testing: ").formatted(Formatting.GOLD)
                    .append(Text.literal("Total: " + testingResult.getTotalTestCount() + ".").formatted(Formatting.WHITE));

            for (TestResult failedTest : testingResult.failed()) {
                if (failedTest.error() != null)
                    context.getSource().sendError(Text.literal(failedTest.name() + " failed: " + failedTest.error())
                            .formatted(Formatting.DARK_RED));
            }

            if (testingResult.passed().size() > 0) {
                message.append(" ");
                message.append(Text.literal("Passed: " + testingResult.passed()
                        .size() + ".").formatted(Formatting.GREEN));
            }

            if (testingResult.failed().size() > 0) {
                message.append(" ");
                message.append(Text.literal("Failed: " + testingResult.failed()
                        .size() + ".").formatted(Formatting.RED));
            }

            if (testingResult.skipped().size() > 0) {
                message.append(" ");
                message.append(Text.literal("Skipped: " + testingResult.skipped()
                        .size() + ".").formatted(Formatting.GRAY));
            }

            if (testingResult.failed().size() == 0)
                context.getSource().sendFeedback(() -> message, false);
            else
                context.getSource().sendError(message);

        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }

        return 0;
    }
}
