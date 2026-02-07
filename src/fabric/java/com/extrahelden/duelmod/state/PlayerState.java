package com.extrahelden.duelmod.state;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerState {
    private static final Map<UUID, Integer> LIVES = new ConcurrentHashMap<>();

    private PlayerState() {
    }

    public static int getLives(ServerPlayerEntity player) {
        return LIVES.computeIfAbsent(player.getUuid(), ignored -> 3);
    }

    public static int setLives(ServerPlayerEntity player, int lives) {
        int normalized = Math.max(0, lives);
        LIVES.put(player.getUuid(), normalized);
        return normalized;
    }

    public static int addLives(ServerPlayerEntity player, int delta) {
        return setLives(player, getLives(player) + delta);
    }

    public static void resetPlayer(ServerPlayerEntity player) {
        LIVES.remove(player.getUuid());
    }
}
