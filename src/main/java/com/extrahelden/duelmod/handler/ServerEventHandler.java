package com.extrahelden.duelmod.handler;

import com.extrahelden.duelmod.DuelMod;
import com.extrahelden.duelmod.helper.Helper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod.EventBusSubscriber(modid = DuelMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {

    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final ZoneId TIME_ZONE = ZoneId.of("Europe/Berlin");
    private static File playtimeFile;
    private static File myLivesFile;
    private static final Gson GSON = new Gson();
    private static final Type MY_LIVES_MAP_TYPE     = new TypeToken<Map<UUID, Integer>>() {}.getType();

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        // Server-Verzeichnis ermitteln
        File baseDir = server.getServerDirectory();
        playtimeFile = new File(baseDir, "player_playtime.json");
        myLivesFile   = new File(baseDir, "my_lives.json");

        // Spielzeit-Datei anlegen, falls nicht vorhanden

        // Lebens-Datei anlegen, falls nicht vorhanden
        if (!myLivesFile.exists()) {
            try {
                if (myLivesFile.createNewFile()) {
                    System.out.println(Helper.getPrefix() + " Lebens-Datei erstellt: " + myLivesFile.getAbsolutePath());
                    saveMyLivesData();
                } else {
                    System.out.println(Helper.getPrefix() + " Konnte Lebens-Datei nicht erstellen: " + myLivesFile.getAbsolutePath());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        // Tägliches Zurücksetzen um Mitternacht
        long initialDelay = calculateInitialDelay();
        long period       = TimeUnit.DAYS.toMillis(1);
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // Daten sichern
        saveMyLivesData();

        // Scheduler sauber herunterfahren
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }

    private static long calculateInitialDelay() {
        LocalDateTime now       = LocalDateTime.now(TIME_ZONE);
        LocalDateTime nextReset = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        if (!now.isBefore(nextReset)) {
            nextReset = nextReset.plusDays(1);
        }
        return Duration.between(now, nextReset).toMillis();
    }

    public static String getTimeUntilReset() {
        LocalDateTime now       = LocalDateTime.now(TIME_ZONE);
        LocalDateTime nextReset = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        if (!now.isBefore(nextReset)) {
            nextReset = nextReset.plusDays(1);
        }
        Duration d = Duration.between(now, nextReset);
        long h = d.toHours();
        long m = d.toMinutes() % 60;
        long s = d.getSeconds() % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    public static void saveMyLivesData(UUID playerUUID, int lives) {
        Map<UUID, Integer> map = loadMyLivesData();
        map.put(playerUUID, lives);
        try (FileWriter writer = new FileWriter(myLivesFile)) {
            GSON.toJson(map, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveMyLivesData() {
        Map<UUID, Integer> map = loadMyLivesData();
        try (FileWriter writer = new FileWriter(myLivesFile)) {
            GSON.toJson(map, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<UUID, Integer> loadMyLivesData() {
        if (!myLivesFile.exists()) {
            return new HashMap<>();
        }
        try (FileReader reader = new FileReader(myLivesFile)) {
            Map<UUID, Integer> map = GSON.fromJson(reader, MY_LIVES_MAP_TYPE);
            return map != null ? map : new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    private static void loadMyLives() {
        if (!myLivesFile.exists()) {
            System.out.println(Helper.getPrefix() + " Lebens-Datei nicht gefunden: " + myLivesFile.getAbsolutePath());
            return;
        }
        try (FileReader reader = new FileReader(myLivesFile)) {
            Map<UUID, Integer> map = GSON.fromJson(reader, MY_LIVES_MAP_TYPE);
            if (map != null) {
                // Alte Daten in neues Format übernehmen (speichert gleich wieder)
                map.forEach(ServerEventHandler::saveMyLivesData);
                System.out.println(Helper.getPrefix() + " Lebens-Daten geladen: " + map.size() + " Einträge.");
            } else {
                System.out.println(Helper.getPrefix() + " Keine Lebens-Daten in Datei.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getMyLives(UUID playerUUID) {
        return loadMyLivesData().getOrDefault(playerUUID, DuelMod.getAnzahlLeben());
    }
}
