package com.extrahelden.duelmod.handler;

import com.extrahelden.duelmod.DuelMod;
import com.extrahelden.duelmod.effect.ModEffects;
import com.extrahelden.duelmod.helper.Helper;
import com.extrahelden.duelmod.util.LinkedHeartOwnerHelper;
import com.extrahelden.duelmod.combat.CombatManager;
import com.extrahelden.duelmod.duel.DuelManager;
import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = DuelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MyForgeEventHandler {

    public static final int COMBAT_SECONDS = 30;

    // =========================
    // LOGIN
    // =========================
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag data = player.getPersistentData();

        // Lives initialisieren/laden
        int lives = data.contains("MyLives") ? data.getInt("MyLives") : DuelMod.ANZAHL_LEBEN;
        if (!data.contains("MyLives")) {
            data.putInt("MyLives", lives);
            ServerEventHandler.saveMyLivesData(player.getUUID(), lives);
        }

        // LinkedHeart Owner (Name) aus Config
        String ownerName = LinkedHeartOwnerHelper.resolveOwnerFor(
                player.getGameProfile().getName(),
                player.getStringUUID()
        );
        if (ownerName == null || ownerName.isBlank()) ownerName = player.getGameProfile().getName();
        data.putString("LinkedHeartOwner", ownerName);

        // Owner-UUID (Cache → Fallback Offline-UUID)
        String ownerUuid = resolveOwnerUuid(player, ownerName);
        data.putString("LinkedHeartOwnerUUID", ownerUuid);

        // Combat-Logout (-1, ohne Kick)
        if (data.contains("CombatLoggedOut")) {
            lives = Math.max(0, lives - 1);
            data.putInt("MyLives", lives);
            ServerEventHandler.saveMyLivesData(player.getUUID(), lives);
            data.remove("CombatLoggedOut");
        }

        // Flag konsistent
        data.putBoolean("LinkedHeartActive", lives == 0);

        final int lf = lives;
        final String on = ownerName, ou = ownerUuid;
        // einen Tick später syncen
        player.server.execute(() -> {
            com.extrahelden.duelmod.network.NetworkHandler.syncLives(player, lf, on, ou);
            // optional noch einmal
            player.server.execute(() -> com.extrahelden.duelmod.network.NetworkHandler.syncLives(player, lf, on, ou));
        });

        if (lives == 0) {
            player.sendSystemMessage(Component.literal(""));
        }

        if (data.getBoolean("LivePrefix")) {
            var board = player.getScoreboard();
            String teamName = "live_" + player.getScoreboardName();
            var team = board.getPlayerTeam(teamName);
            if (team == null) {
                team = board.addPlayerTeam(teamName);
            }
            team.setPlayerPrefix(Component.literal("Live ").withStyle(ChatFormatting.RED));
            board.addPlayerToTeam(player.getScoreboardName(), team);
        }
    }

    // =========================
    // TODES-LOGIK
    // =========================
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;

        if (DuelManager.isInDuel(victim)) {
            ServerPlayer opponent = DuelManager.getOpponent(victim);
            DuelManager.end(victim);
            if (opponent != null) {
            }
            return;
        }

        if (!CombatManager.isInCombat(victim)) return;
        CombatManager.remove(victim);
        com.extrahelden.duelmod.network.NetworkHandler.sendCombatDeath(victim);

        CompoundTag data = victim.getPersistentData();

        // Doppelverarbeitung verhindern
        if (data.getBoolean("DeathHandled")) return;
        data.putBoolean("DeathHandled", true);

        int lives = Math.max(0, data.getInt("MyLives"));

        // Lifesaver blockt
        if (victim.hasEffect(ModEffects.LIFESAVER_EFFECT.get())) {
            victim.removeEffect(ModEffects.LIFESAVER_EFFECT.get());
            victim.sendSystemMessage(Component.literal(Helper.getPrefix() + "§a Du hast kein Leben verloren!"));
            data.putBoolean("SavedByLifesaver", true);
            // Flag unverändert zu Lives
            data.putBoolean("LinkedHeartActive", lives == 0);
            com.extrahelden.duelmod.network.NetworkHandler.syncLives(
                    victim, lives,
                    data.getString("LinkedHeartOwner"),
                    data.getString("LinkedHeartOwnerUUID")
            );
            return;
        }

        // Angreifer ermitteln (direkt, Projektil, Credit, LastHurtBy)
        ServerPlayer attacker = resolveKiller(victim, event);
        String attackerName = (attacker != null)
                ? attacker.getGameProfile().getName()
                : (data.contains("LastAttackerName") ? data.getString("LastAttackerName") : "Unbekannt");

        String ownerName = data.getString("LinkedHeartOwner");
        String ownerUuid = data.getString("LinkedHeartOwnerUUID");
        if (ownerName == null || ownerName.isBlank()) ownerName = victim.getGameProfile().getName();
        if (ownerUuid == null || ownerUuid.isBlank()) ownerUuid = resolveOwnerUuid(victim, ownerName);

        // >1 → -1 Leben
        if (lives > 1) {
            int newLives = lives - 1;
            data.putInt("MyLives", newLives);
            data.putBoolean("LinkedHeartActive", false);
            ServerEventHandler.saveMyLivesData(victim.getUUID(), newLives);

            broadcastKillMessage(victim, attackerName, "§c(-1 Leben)");
            victim.sendSystemMessage(Component.literal(Helper.getPrefix() + "§c Du hast ein Leben verloren!"));

            com.extrahelden.duelmod.network.NetworkHandler.syncLives(victim, newLives, ownerName, ownerUuid);
            return;
        }

        // ==1 → wird 0, Linked Heart aktiv (KEIN Ban jetzt)
        if (lives == 1) {
            data.putInt("MyLives", 0);
            data.putBoolean("LinkedHeartActive", true);
            ServerEventHandler.saveMyLivesData(victim.getUUID(), 0);

            broadcastKillMessage(victim, attackerName, "§c(-1 Leben) §a(Linked Heart aktiv)");
            victim.sendSystemMessage(Component.literal(Helper.getPrefix() + "§e Dein Linked Heart ist jetzt aktiv."));

            com.extrahelden.duelmod.network.NetworkHandler.syncLives(victim, 0, ownerName, ownerUuid);
            return;
        }

        // lives == 0 → nur bannen/kicken wenn Angreifer Spieler ist
        if (attacker != null) {
            broadcastKillMessage(victim, attackerName, "");

            var srv = victim.getServer();
            if (srv != null) {
                GameProfile profile = victim.getGameProfile();
                UserBanList banList = srv.getPlayerList().getBans();
                UserBanListEntry entry = new UserBanListEntry(
                        profile,
                        null,               // ab sofort
                        "EXTRAHELDEN",
                        null,               // permanent
                        Helper.getPrefix() + "\n\nDu hast alle Herzen verloren und bist damit aus dem Projekt §c§l ausgeschieden!"
                );
                banList.add(entry);
            }

            data.putBoolean("LinkedHeartActive", false); // optional
            victim.connection.disconnect(Component.literal(Helper.getPrefix() +
                    "\n\nDu hast alle Herzen verloren und bist damit aus dem Projekt §c§l ausgeschieden!"
            ));
        } else {
            // Umwelt/kein Spieler → kein Ban, Linked Heart bleibt aktiv
            victim.sendSystemMessage(Component.literal(""));
            data.putBoolean("LinkedHeartActive", true);
            com.extrahelden.duelmod.network.NetworkHandler.syncLives(victim, 0, ownerName, ownerUuid);
        }
    }

    private static ServerPlayer resolveKiller(ServerPlayer victim, LivingDeathEvent event) {
        Entity src = event.getSource().getEntity();
        if (src instanceof ServerPlayer sp1) return sp1;

        Entity direct = event.getSource().getDirectEntity();
        if (direct instanceof Projectile proj) {
            Entity owner = proj.getOwner();
            if (owner instanceof ServerPlayer sp2) return sp2;
        }

        Entity credit = victim.getKillCredit();
        if (credit instanceof ServerPlayer sp3) return sp3;

        if (victim.getLastHurtByMob() instanceof ServerPlayer sp4) return sp4;

        return null;
    }

    private static void broadcastKillMessage(ServerPlayer victim, String attackerName, String suffix) {
        MutableComponent msg = Component.literal(Helper.getPrefix() + " ")
                .append(victim.getDisplayName().copy().withStyle(ChatFormatting.RED))
                .append(Component.literal("§r§8 wurde im Kampf von "))
                .append(Component.literal(attackerName == null ? "Unbekannt" : attackerName)
                        .copy().withStyle(ChatFormatting.GREEN))
                .append(Component.literal("§r§8 getötet"));
        if (victim.getServer() != null) {
            victim.getServer().getPlayerList().broadcastSystemMessage(msg, false);
        }
    }

    // =========================
    // LOGOUT WÄHREND COMBAT
    // =========================
    @SubscribeEvent
    public static void onPlayerLogout(PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (DuelManager.isInDuel(player)) {
            DuelManager.end(player);
            return;
        }

        if (!CombatManager.isInCombat(player)) return;
        CombatManager.remove(player);

        CompoundTag data = player.getPersistentData();
        int newLives = Math.max(0, data.getInt("MyLives") - 1);
        data.putInt("MyLives", newLives);
        data.putBoolean("LinkedHeartActive", newLives == 0);
        ServerEventHandler.saveMyLivesData(player.getUUID(), newLives);

        String ownerName = data.getString("LinkedHeartOwner");
        String ownerUuid = data.getString("LinkedHeartOwnerUUID");
        if (ownerName == null || ownerName.isBlank()) ownerName = player.getGameProfile().getName();
        if (ownerUuid == null || ownerUuid.isBlank()) ownerUuid = resolveOwnerUuid(player, ownerName);

        com.extrahelden.duelmod.network.NetworkHandler.syncLives(player, newLives, ownerName, ownerUuid);
        broadcastCombatLogout(player, newLives);
    }

    private static void broadcastCombatLogout(ServerPlayer player, int livesLeft) {
        MutableComponent msg = Component.literal(Helper.getPrefix() + " ")
                .append(player.getDisplayName().copy().withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD))
                .append(Component.literal("§r§8 hat sich während des Kampfes ausgeloggt! §c(-1 Leben)"));
        if (player.getServer() != null) {
            player.getServer().getPlayerList().broadcastSystemMessage(msg, false);
        }
    }

    // =========================
    // CLONE (nach Death)
    // =========================
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        if (!event.isWasDeath()) return;

        var oldData = event.getOriginal().getPersistentData();
        var newData = newPlayer.getPersistentData();

        int lives = oldData.getInt("MyLives");
        String ownerName = oldData.getString("LinkedHeartOwner");
        String ownerUuid = oldData.getString("LinkedHeartOwnerUUID");
        boolean linkedActive = oldData.getBoolean("LinkedHeartActive");

        newData.putInt("MyLives", lives);
        if (ownerName != null) newData.putString("LinkedHeartOwner", ownerName);
        if (ownerUuid != null) newData.putString("LinkedHeartOwnerUUID", ownerUuid);
        newData.putBoolean("LinkedHeartActive", linkedActive);

        newData.remove("CombatLoggedOut");
        newData.remove("DeathHandled");

        if (ownerName == null || ownerName.isBlank()) ownerName = newPlayer.getGameProfile().getName();
        if (ownerUuid == null || ownerUuid.isBlank()) ownerUuid = resolveOwnerUuid(newPlayer, ownerName);

        com.extrahelden.duelmod.network.NetworkHandler.syncLives(newPlayer, lives, ownerName, ownerUuid);
    }

    // =========================
    // RESPAWN (ohne Clone)
    // =========================
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        var data = player.getPersistentData();
        data.remove("DeathHandled");
        data.remove("CombatLoggedOut");

        int livesVal = data.getInt("MyLives");
        String ownerName = data.getString("LinkedHeartOwner");
        String ownerUuid = data.getString("LinkedHeartOwnerUUID");
        if (ownerName == null || ownerName.isBlank()) {
            ownerName = player.getGameProfile().getName();
            data.putString("LinkedHeartOwner", ownerName);
        }
        if (ownerUuid == null || ownerUuid.isBlank()) {
            ownerUuid = resolveOwnerUuid(player, ownerName);
            data.putString("LinkedHeartOwnerUUID", ownerUuid);
        }

        data.putBoolean("LinkedHeartActive", livesVal == 0);

        final int lf = livesVal;
        final String on = ownerName, ou = ownerUuid;
        player.server.execute(() -> {
            com.extrahelden.duelmod.network.NetworkHandler.syncLives(player, lf, on, ou);
            if (lf == 0) {
                player.sendSystemMessage(Component.literal(
                        Helper.getPrefix() + "§a Dein Linked Heart ist aktiv."
                ));
            }
        });
    }

    // =========================
    // COMBAT HANDLING
    // =========================
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim) {
            if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
                if (!DuelManager.isInDuel(attacker) && !DuelManager.isInDuel(victim)) {
                    CombatManager.engage(attacker, victim);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CombatManager.tick();
            var server = event.getServer();
            if (server != null) {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    int ticks = CombatManager.getRemainingTicks(player);
                    if (ticks > 0) {
                        int seconds = (ticks + 19) / 20;
                        player.displayClientMessage(
                                Component.literal("Im Kampf! ").withStyle(ChatFormatting.RED)
                                        .append(Component.literal("(" + seconds + " s übrig)")
                                                .withStyle(ChatFormatting.GRAY)),
                                true);
                    }
                }
            }
        }
    }

    // =========================
    // Hilfen
    // =========================
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
