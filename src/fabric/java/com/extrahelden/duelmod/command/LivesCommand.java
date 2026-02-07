package com.extrahelden.duelmod.command;

import com.extrahelden.duelmod.state.PlayerState;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class LivesCommand {
    private LivesCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("lives")
                .executes(ctx -> {
                    ServerPlayerEntity self = ctx.getSource().getPlayerOrThrow();
                    int lives = PlayerState.getLives(self);
                    ctx.getSource().sendFeedback(() -> Text.literal("Deine Leben: " + lives), false);
                    return lives;
                })
                .then(CommandManager.argument("value", IntegerArgumentType.integer(0, 99))
                        .requires(source -> source.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerPlayerEntity self = ctx.getSource().getPlayerOrThrow();
                            int value = IntegerArgumentType.getInteger(ctx, "value");
                            PlayerState.setLives(self, value);
                            ctx.getSource().sendFeedback(() -> Text.literal("Leben gesetzt auf: " + value), true);
                            return 1;
                        })));
    }
}
