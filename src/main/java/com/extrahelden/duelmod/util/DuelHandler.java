package com.extrahelden.duelmod.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class DuelHandler {
    private static final Map<UUID, UUID> pendingDuels = new HashMap<>();
    private static final Map<UUID, UUID> activePairs = new HashMap<>();
    private static final Map<UUID, Long> startTimes = new HashMap<>();

    public static void challengePlayer(ServerPlayer challenger, ServerPlayer target) {
        pendingDuels.put(target.getUUID(), challenger.getUUID());
        target.sendSystemMessage(Component.literal(challenger.getName().getString() + " hat dich zu einem Duell herausgefordert. Gib /accept ein!"));
        challenger.sendSystemMessage(Component.literal("Du hast " + target.getName().getString() + " herausgefordert."));
    }

    public static boolean hasPending(UUID target) {
        return pendingDuels.containsKey(target);
    }

    public static UUID consumePending(UUID target) {
        return pendingDuels.remove(target);
    }

    public static void startDuel(ServerPlayer a, ServerPlayer b) {
        activePairs.put(a.getUUID(), b.getUUID());
        activePairs.put(b.getUUID(), a.getUUID());
        long now = System.currentTimeMillis();
        startTimes.put(a.getUUID(), now);
        startTimes.put(b.getUUID(), now);
    }

    public static void endDuel(UUID a, UUID b) {
        activePairs.remove(a);
        activePairs.remove(b);
        startTimes.remove(a);
        startTimes.remove(b);
    }

    public static boolean isInDuel(UUID id) {
        return activePairs.containsKey(id);
    }

    public static long getStartTime(UUID id) {
        return startTimes.getOrDefault(id, 0L);
    }

    public static Collection<UUID> getOpponents(UUID id) {
        UUID opp = activePairs.get(id);
        return opp == null ? Collections.emptyList() : Collections.singleton(opp);
    }
}