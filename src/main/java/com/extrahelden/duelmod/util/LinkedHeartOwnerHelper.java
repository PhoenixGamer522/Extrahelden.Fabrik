package com.extrahelden.duelmod.util;



import java.util.List;

public class LinkedHeartOwnerHelper {

    /**
     * Bestimmt, welcher Spieler als "Owner" für das Linked Heart gilt.
     *
     * @param playerName   aktueller Spielername
     * @param playerUuid   aktuelle UUID (als String)
     * @return Owner-Name (z. B. "CubeKingdom"), oder fallback = eigener Name
     */
    public static String resolveOwnerFor(String playerName, String playerUuid) {
        String needleName = "name:" + playerName.toLowerCase();
        String needleUuid = "uuid:" + playerUuid.toLowerCase();

        List<? extends String> entries = DuelConfig.COMMON.ownerOverrides.get();

        if (entries != null) {
            for (String raw : entries) {
                if (raw == null || raw.isBlank()) continue;
                String[] parts = raw.split("->", 2);
                if (parts.length != 2) continue;

                String left = parts[0].trim().toLowerCase();
                String right = parts[1].trim();

                if (left.equals(needleName) || left.equals(needleUuid)) {
                    return right;
                }
            }
        }
        return playerName;
    }
}
