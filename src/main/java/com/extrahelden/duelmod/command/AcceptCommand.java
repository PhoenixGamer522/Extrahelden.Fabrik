package com.extrahelden.duelmod.command;

import com.extrahelden.duelmod.combat.CombatManager;
import com.extrahelden.duelmod.duel.DuelManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class AcceptCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("accept")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    UUID chalId = DuelManager.getPending(player);
                    if (chalId == null) {
                        player.sendSystemMessage(Component.literal("Du hast keine Duel-Anfragen."));
                        return 1;
                    }
                    ServerPlayer challenger = player.getServer().getPlayerList().getPlayer(chalId);
                    if (challenger == null) {
                        DuelManager.deny(player);
                        player.sendSystemMessage(Component.literal("Herausforderer nicht mehr online."));
                        return 1;
                    }
                    if (DuelManager.accept(player)) {
                        CombatManager.remove(challenger);
                        CombatManager.remove(player);
                        player.sendSystemMessage(Component.literal("Duel mit "
                                + challenger.getGameProfile().getName() + " gestartet."));
                        challenger.sendSystemMessage(Component.literal(player.getGameProfile().getName()
                                + " hat das Duel angenommen."));
                    }
                    return 1;
                }));
    }
}
