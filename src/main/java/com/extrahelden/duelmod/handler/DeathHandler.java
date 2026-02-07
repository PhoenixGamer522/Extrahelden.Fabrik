package com.extrahelden.duelmod.handler;

import com.extrahelden.duelmod.DuelMod;
import com.extrahelden.duelmod.item.ModItems;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

@Mod.EventBusSubscriber(modid = DuelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DeathHandler {

    private static File deathFile;
    private static final Gson GSON = new Gson();
    private static final Type TYPE = new TypeToken<Map<UUID, List<BlockPos>>>() {}.getType();
    private static Map<UUID, List<BlockPos>> deathPositions = new HashMap<>();

    // ---------------- Speicher/Load ----------------
    public static void load(MinecraftServer server) {
        deathFile = new File(server.getServerDirectory(), "death_positions.json");
        if (!deathFile.exists()) {
            deathPositions = new HashMap<>();
            return;
        }
        try (FileReader reader = new FileReader(deathFile)) {
            Map<UUID, List<BlockPos>> loaded = GSON.fromJson(reader, TYPE);
            deathPositions = (loaded != null) ? loaded : new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
            deathPositions = new HashMap<>();
        }
    }

    public static void save(MinecraftServer server) {
        if (deathFile == null) {
            deathFile = new File(server.getServerDirectory(), "death_positions.json");
        }
        try (FileWriter writer = new FileWriter(deathFile)) {
            GSON.toJson(deathPositions, TYPE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addDeathPosition(UUID playerUUID, BlockPos pos) {
        List<BlockPos> list = deathPositions.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        list.add(0, pos);
        if (list.size() > 3) {
            list.remove(list.size() - 1);
        }
    }

    public static List<BlockPos> getDeathPositions(UUID playerUUID) {
        return Collections.unmodifiableList(
                deathPositions.getOrDefault(playerUUID, Collections.emptyList())
        );
    }

    public static void removeDeathPosition(BlockPos pos) {
        for (List<BlockPos> list : deathPositions.values()) {
            list.remove(pos);
        }
    }
    // ---------------- HEART STEALER LOGIK ----------------
    @SubscribeEvent
    public static void onPlayerKill(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        ItemStack weapon = killer.getMainHandItem();
        if (weapon.isEmpty() || weapon.getItem() != ModItems.HEART_STEALER.get()) {
            return; // Kein HeartStealer benutzt
        }

        killer.sendSystemMessage(Component.literal("§cDu hast ein Herz von " + victim.getName().getString() + " gestohlen!"));
        victim.sendSystemMessage(Component.literal("§4Ein Herz wurde dir vom HeartStealer geraubt!"));
    }
}
