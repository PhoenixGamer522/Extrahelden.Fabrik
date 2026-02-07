// LinkedHeartHelper.java
package com.extrahelden.duelmod.helper;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LinkedHeartHelper {
    private static final Map<String, ResourceLocation> CACHE = new ConcurrentHashMap<>();
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();

    private LinkedHeartHelper() {}

    /** Holt Skin-Resource für Owner (Name+UUID aus NBT). */
    public static ResourceLocation getSkinFor(String ownerName, String ownerUuid, Minecraft mc, Player localPlayer) {
        // fallback, wenn nichts gesetzt
        if ((ownerName == null || ownerName.isBlank()) && localPlayer != null) {
            return skinOf(localPlayer);
        }
        final String key = ((ownerUuid != null && !ownerUuid.isBlank()) ? ownerUuid : ownerName).toLowerCase();

        // Cache?
        var cached = CACHE.get(key);
        if (cached != null) return cached;

        // Versuche: wenn Owner online ist, echte Skin sofort
        if (mc.level != null && ownerName != null) {
            for (Player p : mc.level.players()) {
                if (p.getName().getString().equalsIgnoreCase(ownerName)) {
                    var loc = skinOf(p);
                    CACHE.put(key, loc);
                    return loc;
                }
            }
        }

        // Asynchron über SkinManager laden (UUID bevorzugen)
        if (PENDING.add(key)) {
            UUID uuid = null;
            try {
                if (ownerUuid != null && !ownerUuid.isBlank()) uuid = UUID.fromString(ownerUuid);
            } catch (Exception ignored) {}
            GameProfile profile = new GameProfile(uuid, ownerName == null ? "" : ownerName);

            SkinManager sm = mc.getSkinManager();
            // requireSecure = true reicht mit echter UUID, sonst false probieren
            sm.registerSkins(profile, (type, loc, tex) -> {
                if (type == MinecraftProfileTexture.Type.SKIN && loc != null) {
                    CACHE.put(key, loc);
                }
            }, uuid == null ? false : true);
        }

        // Bis die echte Skin da ist → Default abhängig von UUID
        UUID fallback = (ownerUuid != null && !ownerUuid.isBlank())
                ? tryParse(ownerUuid)
                : offlineUuid(ownerName == null ? "Steve" : ownerName);
        return DefaultPlayerSkin.getDefaultSkin(fallback);
    }

    private static ResourceLocation skinOf(Player p) {
        if (p instanceof AbstractClientPlayer acp) return acp.getSkinTextureLocation();
        return DefaultPlayerSkin.getDefaultSkin(p.getUUID());
    }

    private static UUID tryParse(String s) {
        try { return UUID.fromString(s); } catch (Exception e) { return offlineUuid(s); }
    }

    private static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name)
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static void clearCache() {
        CACHE.clear();
        PENDING.clear();
    }
}
