package com.extrahelden.duelmod.duel;

import com.extrahelden.duelmod.helper.Helper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.extrahelden.duelmod.network.NetworkHandler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks duel requests and active duels between players.
 */
public final class DuelManager {
    private static final Map<UUID, UUID> PENDING = new ConcurrentHashMap<>(); // target -> challenger
    private static final Map<UUID, UUID> ACTIVE = new ConcurrentHashMap<>(); // player -> opponent
    private static final Map<UUID, Integer> USES = new ConcurrentHashMap<>(); // player -> /duel uses

    private DuelManager() {}

    public static boolean canChallenge(ServerPlayer player) {
        return USES.getOrDefault(player.getUUID(), 0) < 3;
    }

    public static void recordUse(ServerPlayer player) {
        USES.merge(player.getUUID(), 1, Integer::sum);
    }

    public static void request(ServerPlayer challenger, ServerPlayer target) {
        PENDING.put(target.getUUID(), challenger.getUUID());
    }

    public static UUID getPending(ServerPlayer target) {
        return PENDING.get(target.getUUID());
    }

    public static void deny(ServerPlayer target) {
        PENDING.remove(target.getUUID());
    }

    public static boolean accept(ServerPlayer target) {
        UUID chalId = PENDING.remove(target.getUUID());
        if (chalId == null) return false;
        ServerPlayer challenger = target.getServer().getPlayerList().getPlayer(chalId);
        if (challenger == null) return false;

        ACTIVE.put(chalId, target.getUUID());
        ACTIVE.put(target.getUUID(), chalId);

        challenger.getPersistentData().putBoolean("InDuel", true);
        target.getPersistentData().putBoolean("InDuel", true);

        NetworkHandler.sendDuelStatus(challenger, true);
        NetworkHandler.sendDuelStatus(target, true);

        // --- Enderperlen-Limit anwenden ---
        limitEnderpearls(challenger);
        limitEnderpearls(target);

        return true;
    }

    public static boolean isInDuel(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static ServerPlayer getOpponent(ServerPlayer player) {
        UUID opp = ACTIVE.get(player.getUUID());
        if (opp == null) return null;
        return player.getServer().getPlayerList().getPlayer(opp);
    }

    public static void end(ServerPlayer player) {
        UUID oppId = ACTIVE.remove(player.getUUID());
        if (oppId != null) {
            ACTIVE.remove(oppId);
            ServerPlayer opp = player.getServer().getPlayerList().getPlayer(oppId);
            if (opp != null) {
                opp.getPersistentData().putBoolean("InDuel", false);
                NetworkHandler.sendDuelStatus(opp, false);
                opp.sendSystemMessage(Component.literal(Helper.getPrefix() + "§8 Duel wurde beendet"));
            }
        }
        player.getPersistentData().putBoolean("InDuel", false);
        NetworkHandler.sendDuelStatus(player, false);
    }

    /** Begrenzt die Enderperlen eines Spielers auf maximal 3 Stacks (48 Stück). */
    private static void limitEnderpearls(ServerPlayer player) {
        int pearls = 0;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.getItem() == Items.ENDER_PEARL) {
                pearls += stack.getCount();
                if (pearls > 48) {
                    int overflow = pearls - 48;
                    int newCount = stack.getCount() - overflow;
                    stack.setCount(Math.max(newCount, 0));
                    player.getInventory().items.set(i, stack);
                    pearls = 48;
                }
            }
        }
        player.displayClientMessage(
                Component.literal("§eDeine Enderperlen wurden im Duel auf maximal 3 Stacks (48) begrenzt."),
                true
        );
    }
}
