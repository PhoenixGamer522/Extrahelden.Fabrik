package com.extrahelden.duelmod.command;

import com.extrahelden.duelmod.client.overlay.SpectatorHudOverlay;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.Map;

public class SpectateCommand {

    // Speichert den vorherigen Gamemode, damit man zurückgesetzt werden kann
    private static final Map<ServerPlayer, GameType> previousGamemodes = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vanish")
                .then(Commands.literal("spectate")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    String targetName = StringArgumentType.getString(ctx, "target");

                                    if (targetName.equalsIgnoreCase("off")) {
                                        stopSpectating(player);
                                        return 1;
                                    }

                                    MinecraftServer server = ctx.getSource().getServer();
                                    ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);

                                    if (target == null) {
                                        player.sendSystemMessage(Component.literal("Spieler nicht gefunden!")
                                                .withStyle(ChatFormatting.RED));
                                        return 0;
                                    }

                                    startSpectating(player, target);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("off")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    stopSpectating(player);
                                    return 1;
                                })
                        )
                )
        );
    }

    private static void startSpectating(ServerPlayer player, ServerPlayer target) {
        if (!previousGamemodes.containsKey(player)) {
            previousGamemodes.put(player, player.gameMode.getGameModeForPlayer());
        }

        player.setGameMode(GameType.SPECTATOR);
        player.setCamera(target);

        // HUD-Overlay aktivieren
        SpectatorHudOverlay.setSpectated(target);

        player.sendSystemMessage(Component.literal("Du spectatest nun " + target.getName().getString() + ".")
                .withStyle(ChatFormatting.GRAY));
    }

    private static void stopSpectating(ServerPlayer player) {
        if (previousGamemodes.containsKey(player)) {
            GameType oldMode = previousGamemodes.remove(player);
            player.setGameMode(oldMode);
            player.setCamera(player);
        } else {
            player.setCamera(player);
            player.setGameMode(GameType.SURVIVAL); // Fallback
        }

        // HUD-Overlay deaktivieren
        SpectatorHudOverlay.clearSpectated();

        player.sendSystemMessage(Component.literal("Spectator-Modus beendet.")
                .withStyle(ChatFormatting.GRAY));
    }
}
