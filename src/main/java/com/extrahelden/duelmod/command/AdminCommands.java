package com.extrahelden.duelmod.command;

import com.extrahelden.duelmod.vanish.VanishManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class AdminCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vanish")
                .requires(source -> source.hasPermission(2)) // nur OPs
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    boolean vanished = VanishManager.toggleVanish(player);

                    if (vanished) {
                        player.sendSystemMessage(Component.literal("§aDu bist nun im Vanish-Modus!"));
                    } else {
                        player.sendSystemMessage(Component.literal("§cDu bist nun wieder sichtbar!"));
                    }
                    return 1;
                })
                .then(Commands.argument("state", BoolArgumentType.bool())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            boolean state = BoolArgumentType.getBool(ctx, "state");

                            if (state) {
                                VanishManager.hidePlayer(player);
                                player.sendSystemMessage(Component.literal("§aDu bist nun im Vanish-Modus!"));
                            } else {
                                VanishManager.showPlayer(player);
                                player.sendSystemMessage(Component.literal("§cDu bist nun wieder sichtbar!"));
                            }
                            return 1;
                        })
                )
        );
    }
}
