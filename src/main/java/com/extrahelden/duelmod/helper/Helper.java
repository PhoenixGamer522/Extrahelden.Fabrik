package com.extrahelden.duelmod.helper;

import java.util.concurrent.TimeUnit;

public class Helper {
    private static String prefix = "[§bExtrahelden§l§f]";
    private static String projectName = "Minecraft Extrahelden";

    public static String formatTime(int var0) {
        long var1 = TimeUnit.SECONDS.toHours((long)var0);
        long var3 = TimeUnit.SECONDS.toMinutes((long)var0) % 60L;
        long var5 = (long)(var0 % 60);
        if (var1 > 0L) {
            return String.format("%02d:%02d:%02d", var1, var3, var5);
        } else {
            return var3 > 0L ? String.format("%02d:%02d", var3, var5) : String.format("%02d Sekunden", var5);
        }
    }

    public static String getPrefix() {
        return prefix;
    }

    public static String getProjectName() {
        return projectName;
    }
}
