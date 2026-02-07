package com.extrahelden.duelmod.events;

import com.extrahelden.duelmod.DuelMod;
import com.extrahelden.duelmod.handler.DummyManager;
import com.extrahelden.duelmod.combat.CombatTimer;
import com.extrahelden.duelmod.limiter.CombatPearlLimiter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Mod.EventBusSubscriber(modid = DuelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommonEvents {

    private static final Map<LivingEntity, CombatTimer> timers = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static final Map<BlockPos, Long> graves = new HashMap<>();
    private static final Map<BlockPos, Integer> experienceMap = new HashMap<>();
    private static final Map<UUID, LinkedList<BlockPos>> deathPositions = new HashMap<>();
    private static final DummyManager dummyManager = new DummyManager();

    // ----------------- Player Clone (bei Respawn) -----------------
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newP)) return;
        if (!event.isWasDeath()) return;

        var old = event.getOriginal().getPersistentData();
        var data = newP.getPersistentData();

        int lives = old.getInt("MyLives");

        String ownerName = old.getString("LinkedHeartOwner");
        if (ownerName == null || ownerName.isBlank()) {
            ownerName = newP.getGameProfile().getName();
        }

        String ownerUuid = old.getString("LinkedHeartOwnerUUID");
        if (ownerUuid == null || ownerUuid.isBlank()) {
            ownerUuid = resolveOwnerUuid(newP, ownerName);
        }

        data.putInt("MyLives", lives);
        data.putString("LinkedHeartOwner", ownerName);
        data.putString("LinkedHeartOwnerUUID", ownerUuid);
    }

    // ----------------- Respawn -----------------
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;

        var data = p.getPersistentData();
        int lives = data.getInt("MyLives");

        String ownerName = data.getString("LinkedHeartOwner");
        if (ownerName == null || ownerName.isBlank()) ownerName = p.getGameProfile().getName();

        String ownerUuid = data.getString("LinkedHeartOwnerUUID");
        if (ownerUuid == null || ownerUuid.isBlank()) {
            ownerUuid = resolveOwnerUuid(p, ownerName);
            data.putString("LinkedHeartOwnerUUID", ownerUuid);
        }
    }

    // ----------------- Death -----------------
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        boolean byPlayer = e.getSource().getEntity() instanceof ServerPlayer;

        // Combat resetten beim Tod
        CombatPearlLimiter.resetPearlUses(sp);

        // hier könntest du CombatDeath-Animation triggern
        // NetworkHandler.sendKilledByPlayer(sp, byPlayer);
    }

    // ----------------- Angriff = Combat starten -----------------
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer victim)) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer attacker)) return;

        // Beide in Combat setzen
        victim.getPersistentData().putBoolean("InCombat", true);
        attacker.getPersistentData().putBoolean("InCombat", true);

        // Pearl-Zähler zurücksetzen, falls neuer Combat beginnt
        CombatPearlLimiter.resetPearlUses(victim);
        CombatPearlLimiter.resetPearlUses(attacker);
    }

    // ----------------- Hilfsfunktion -----------------
    private static String resolveOwnerUuid(ServerPlayer context, String ownerName) {
        if (ownerName == null || ownerName.isBlank()) return "";
        var srv = context.getServer();
        if (srv != null) {
            var opt = srv.getProfileCache().get(ownerName);
            if (opt.isPresent() && opt.get().getId() != null) {
                return opt.get().getId().toString();
            }
        }
        UUID off = UUID.nameUUIDFromBytes(("OfflinePlayer:" + ownerName)
                .getBytes(StandardCharsets.UTF_8));
        return off.toString();
    }
}
