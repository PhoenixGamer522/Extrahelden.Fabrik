package com.extrahelden.duelmod.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class DuelUtils {
    public static ServerPlayer findNearbyPlayerInSight(ServerPlayer player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<Player> players = player.level().getEntitiesOfClass(Player.class, box,
                p -> p != player && player.hasLineOfSight(p));

        return players.isEmpty() ? null : (ServerPlayer) players.get(0);
    }
}

