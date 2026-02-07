package com.extrahelden.duelmod.handler;

import com.extrahelden.duelmod.mobs.DummyEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Tuple;

import java.util.*;

@EventBusSubscriber
public class DummyManager {

    private static final List<DummyEntity> dummies = new ArrayList<>();
    private static final Map<UUID, DummyStatus> playerDummyStatus = new HashMap<>();

    /**
     * Spawns a dummy copy of the given player in their current world,
     * clones inventory and equipment, and begins a short combat timer.
     */
    public void spawnDummy(Player player) {
        ServerLevel world = (ServerLevel) player.level();
        DummyEntity dummy = new DummyEntity(ModEntities.DUMMY_ENTITY.get(), world);

        // Position & profile
        dummy.setPos(player.getX(), player.getY(), player.getZ());
        dummy.setGameProfile(player.getGameProfile());
        dummy.setPlayerUUID(player.getUUID());
        dummy.setPlayerName(player.getName());
        dummy.setHealth(player.getHealth());

        // Inventory & equipment
        dummy.storeInventory(player.getInventory(), player);
        cloneEquipment(player, dummy);

        // Combat timer so dummy will despawn after a bit

        // Track
        dummies.add(dummy);
        playerDummyStatus.put(player.getUUID(), DummyStatus.ALIVE);

        // Actually spawn
        world.addFreshEntity(dummy);
    }

    /** Copies all armor and held items from the player to the dummy. */
    private void cloneEquipment(Player player, DummyEntity dummy) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            dummy.setItemSlot(slot, player.getItemBySlot(slot));
        }
    }

    /**
     * Returns the dummy and its status (ALIVE, DEAD, or NONE)
     * for a given player UUID.
     */
    public static Tuple<DummyEntity, DummyStatus> findDummyByPlayerUUID(UUID playerUUID) {
        DummyStatus status = playerDummyStatus.getOrDefault(playerUUID, DummyStatus.NONE);
        DummyEntity found = null;
        for (DummyEntity d : dummies) {
            if (playerUUID.equals(d.getPlayerUUID())) {
                found = d;
                break;
            }
        }
        return new Tuple<>(found, status);
    }

    /**
     * Every server tick, check each dummy:
     * - Remove it immediately if it lost its data.
     * - Otherwise, if its combat timer expired, discard it.
     */
    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != Phase.END) return;

        Iterator<DummyEntity> it = dummies.iterator();
        while (it.hasNext()) {
            DummyEntity dummy = it.next();

            // If dummy lost its linkage to a player, discard now
            if (dummy.getPlayerUUID() == null || dummy.getPlayerName() == null) {
                dummy.remove(RemovalReason.DISCARDED);
                it.remove();
                continue;
            }
        }
    }

    /** Marks that a dummy for this player died while they were offline. */
    public static void setPlayerDummyStatusDead(DummyEntity entity) {
        playerDummyStatus.put(entity.getPlayerUUID(), DummyStatus.DEAD);
    }

    /** Clears any dummy‑status entry for the given player. */
    public static void removePlayerDummyStatus(UUID playerUUID) {
        playerDummyStatus.remove(playerUUID);
    }

    /** Removes a dummy instance immediately (e.g. on player reconnect). */
    public static void removeDummy(DummyEntity dummy) {
        dummies.remove(dummy);
        dummy.remove(RemovalReason.DISCARDED);
    }

    /** Possible statuses for a player's dummy. */
    public enum DummyStatus {
        ALIVE,
        DEAD,
        NONE
    }
}