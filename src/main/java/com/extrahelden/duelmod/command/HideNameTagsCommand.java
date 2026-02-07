package com.extrahelden.duelmod.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public class HideNameTagsCommand {

    private static final String TEAM_NAME = "hidden_nametags";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("hidetags")
                        .requires(cs -> cs.hasPermission(2))
                        .executes(ctx -> {
                            hide(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§aAlle Nametags wurden ausgeblendet."),
                                    true
                            );
                            return 1;
                        })
        );

        dispatcher.register(
                Commands.literal("showtags")
                        .requires(cs -> cs.hasPermission(2))
                        .executes(ctx -> {
                            show(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("§aAlle Nametags werden wieder angezeigt."),
                                    true
                            );
                            return 1;
                        })
        );
    }

    private static void hide(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();

        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM_NAME);
        }

        team.setNameTagVisibility(Team.Visibility.NEVER);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
        }
    }

    private static void show(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();

        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team == null) return;

        team.setNameTagVisibility(Team.Visibility.ALWAYS);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            scoreboard.removePlayerFromTeam(player.getScoreboardName(), team);
        }
    }
}
