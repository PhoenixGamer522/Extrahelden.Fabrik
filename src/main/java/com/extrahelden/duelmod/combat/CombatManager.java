package com.extrahelden.duelmod.combat;

import com.extrahelden.duelmod.DuelMod;
import com.extrahelden.duelmod.limiter.CombatPearlLimiter;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages combat timers for players.
 */
public final class CombatManager {
    private static final Map<UUID, CombatTimer> TIMERS = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> PARTNERS = new ConcurrentHashMap<>();

    // 30 Sekunden = 30 * 20 Ticks
    public static final int EXTEND_TICKS = 20 * 30;
    // 500 Sekunden = 500 * 20 Ticks
    public static final int MAX_COMBAT_TICKS = 20 * 500;

    private CombatManager() {
    }

    private static CombatTimer extendTimer(ServerPlayer player) {
        return TIMERS.compute(player.getUUID(), (uuid, existing) -> {
            if (existing == null) {
                DuelMod.LOGGER.debug("Creating new combat timer for {}: {} ticks ({}s)",
                        player.getGameProfile().getName(), EXTEND_TICKS, EXTEND_TICKS / 20);
                return new CombatTimer(EXTEND_TICKS);
            }
            int before = existing.getTicks();
            int newTicks = Math.min(before + EXTEND_TICKS, MAX_COMBAT_TICKS);

            DuelMod.LOGGER.debug("Extending timer for {}: before={} ticks ({}s), add={} ticks ({}s), after={} ticks ({}s), capped={}",
                    player.getGameProfile().getName(),
                    before, before / 20,
                    EXTEND_TICKS, EXTEND_TICKS / 20,
                    newTicks, newTicks / 20,
                    newTicks == MAX_COMBAT_TICKS);

            return new CombatTimer(newTicks);
        });
    }

    /**
     * Put both players into combat or extend their timers and link them as combat partners.
     */
    public static void engage(ServerPlayer a, ServerPlayer b) {
        CombatTimer ta = extendTimer(a);
        CombatTimer tb = extendTimer(b);

        PARTNERS.put(a.getUUID(), b.getUUID());
        PARTNERS.put(b.getUUID(), a.getUUID());

        DuelMod.LOGGER.debug("Player {} is in combat with {} ({} ticks remaining / {}s)",
                a.getGameProfile().getName(), b.getGameProfile().getName(), ta.getTicks(), ta.getSeconds());
        DuelMod.LOGGER.debug("Player {} is in combat with {} ({} ticks remaining / {}s)",
                b.getGameProfile().getName(), a.getGameProfile().getName(), tb.getTicks(), tb.getSeconds());
    }

    /**
     * Check if the player currently has an active combat timer.
     */
    public static boolean isInCombat(ServerPlayer player) {
        CombatTimer timer = TIMERS.get(player.getUUID());
        return timer != null && timer.isActive();
    }

    /**
     * Get remaining ticks of combat for the given player.
     */
    public static int getRemainingTicks(ServerPlayer player) {
        CombatTimer timer = TIMERS.get(player.getUUID());
        return timer != null ? timer.getTicks() : 0;
    }

    /**
     * Tick all combat timers and remove expired ones.
     */
    public static void tick() {
        Iterator<Map.Entry<UUID, CombatTimer>> it = TIMERS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, CombatTimer> entry = it.next();
            if (!entry.getValue().tick()) {
                UUID id = entry.getKey();
                it.remove();
                UUID partner = PARTNERS.remove(id);
                if (partner != null) {
                    UUID back = PARTNERS.get(partner);
                    if (back != null && back.equals(id)) {
                        PARTNERS.remove(partner);
                    }
                }
                // Combat vorbei → Enderperlen-Limit resetten
                CombatPearlLimiter.resetPearlUses(id);
            }
        }

        // Pearl-Limiter tick synchron zum Combat
        CombatPearlLimiter.tick();
    }

    /**
     * Remove a player's combat timer.
     */
    public static void remove(ServerPlayer player) {
        UUID id = player.getUUID();
        TIMERS.remove(id);
        UUID partner = PARTNERS.remove(id);
        if (partner != null) {
            TIMERS.remove(partner);
            UUID back = PARTNERS.get(partner);
            if (back != null && back.equals(id)) {
                PARTNERS.remove(partner);
            }
        }
        // Auch hier sicherheitshalber Perlen resetten
        CombatPearlLimiter.resetPearlUses(id);
    }
}
