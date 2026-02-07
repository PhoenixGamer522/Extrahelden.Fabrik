package com.extrahelden.duelmod.command;

import com.extrahelden.duelmod.duel.DuelManager;
import com.extrahelden.duelmod.helper.Helper;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class DuelCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("duel")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!DuelManager.canChallenge(player)) {
                        player.sendSystemMessage(Component.literal(Helper.getPrefix() + "§l§8 Du kannst keine weiteren Duelle starten."));
                        return 1;
                    }
                    ServerPlayer target = findTarget(player);
                    if (target == null) {
                        player.sendSystemMessage(Component.literal(Helper.getPrefix() + "§l§8 Kein Spieler vor dir."));
                        return 1;
                    }
                    if (DuelManager.isInDuel(player) || DuelManager.isInDuel(target)) {
                        player.sendSystemMessage(Component.literal(Helper.getPrefix() + "§l§8 Einer der Spieler ist bereits in einem Duel."));
                        return 1;
                    }
                    DuelManager.request(player, target);
                    DuelManager.recordUse(player);
                    player.sendSystemMessage(Component.literal("§8Du hast "
                            + target.getGameProfile().getName() + "§8 zu einem Duel herausgefordert."));
                    target.sendSystemMessage(Component.literal(Helper.getPrefix() + player.getGameProfile().getName()
                            + " §l§8 hat dich zu einem Duel herausgefordert. Verwende §l§a/accept zum Annehmen §l§8oder §l§c/deny zum Ablehnen."));
                    return 1;
                }));
    }

    private static ServerPlayer findTarget(ServerPlayer source) {
        double maxDist = 5.0;
        Vec3 eye = source.getEyePosition();
        Vec3 look = source.getLookAngle();
        ServerPlayer result = null;
        double best = maxDist;
        for (ServerPlayer other : source.server.getPlayerList().getPlayers()) {
            if (other == source) continue;
            Vec3 to = other.getEyePosition().subtract(eye);
            double dist = to.length();
            if (dist <= maxDist) {
                to = to.normalize();
                if (to.dot(look) > 0.8 && dist < best) {
                    result = other;
                    best = dist;
                }
            }
        }
        return result;
    }
}
