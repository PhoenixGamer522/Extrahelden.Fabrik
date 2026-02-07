package com.extrahelden.duelmod.vanish;

import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VanishManager {

    private static final Set<ServerPlayer> vanishedPlayers = new HashSet<>();

    // Spieler verschwinden lassen (ECHTES VANISH – kein Chat)
    public static void hidePlayer(ServerPlayer player) {
        vanishedPlayers.add(player);

        // Eigene Sounds muten
        player.setSilent(true);

        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (!other.getUUID().equals(player.getUUID())) {

                // Aus Welt entfernen
                other.connection.send(
                        new ClientboundRemoveEntitiesPacket(player.getId())
                );

                // Aus Tablist entfernen
                other.connection.send(
                        new ClientboundPlayerInfoRemovePacket(
                                List.of(player.getUUID())
                        )
                );
            }
        }
    }

    // Spieler wieder sichtbar machen (ECHTES SHOW – kein Chat)
    public static void showPlayer(ServerPlayer player) {
        vanishedPlayers.remove(player);

        // Sounds wieder aktivieren
        player.setSilent(false);

        for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
            if (!other.getUUID().equals(player.getUUID())) {

                // In Welt hinzufügen
                other.connection.send(
                        new ClientboundAddPlayerPacket(player)
                );

                // In Tablist hinzufügen
                other.connection.send(
                        ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(
                                List.of(player)
                        )
                );
            }
        }
    }

    // Abfragen
    public static boolean isVanished(ServerPlayer player) {
        return vanishedPlayers.contains(player);
    }

    // Umschalten
    public static boolean toggleVanish(ServerPlayer player) {
        if (isVanished(player)) {
            showPlayer(player);
            return false;
        } else {
            hidePlayer(player);
            return true;
        }
    }
}
