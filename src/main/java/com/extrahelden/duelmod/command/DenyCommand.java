package com.extrahelden.duelmod.command;

import com.extrahelden.duelmod.duel.DuelManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class DenyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("deny")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    UUID chalId = DuelManager.getPending(player);
                    if (chalId == null) {
                        player.sendSystemMessage(Component.literal("Keine Duel-Anfrage."));
                        return 1;
                    }
                    DuelManager.deny(player);
                    ServerPlayer challenger = player.getServer().getPlayerList().getPlayer(chalId);
                    if (challenger != null) {
                        challenger.sendSystemMessage(Component.literal(player.getGameProfile().getName()
                                + " hat dein Duel abgelehnt."));
                    }
                    player.sendSystemMessage(Component.literal("Duel abgelehnt."));
                    return 1;
                }));
    }
}
