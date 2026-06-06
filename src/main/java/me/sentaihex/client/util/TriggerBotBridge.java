package me.sentaihex.client.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cross-JVM bridge for TriggerBot.
 * Launcher writes config → Agent reads it and executes attacks inside Minecraft JVM.
 *
 * File: ~/.sentaihex/triggerbot.txt
 * Format: enabled|weaponSlot|delayMs
 * Example: true|-1|100
 */
public final class TriggerBotBridge {

    private static final Path DIR  = Paths.get(System.getProperty("user.home"), ".sentaihex");
    private static final Path FILE = DIR.resolve("triggerbot.txt");

    private TriggerBotBridge() {}

    // --- Launcher side: write ---
    public static void writeConfig(boolean enabled, int weaponSlot, int delayMs) {
        try {
            Files.createDirectories(DIR);
            Files.writeString(FILE, enabled + "|" + weaponSlot + "|" + delayMs);
        } catch (Exception ignored) {}
    }

    // --- Agent side: read ---
    public static boolean isEnabled() {
        return readField(0, "false").equalsIgnoreCase("true");
    }

    public static int getWeaponSlot() {
        try { return Integer.parseInt(readField(1, "-1")); }
        catch (NumberFormatException e) { return -1; }
    }

    public static int getDelayMs() {
        try {
            int d = Integer.parseInt(readField(2, "100"));
            return Math.max(20, d);
        } catch (NumberFormatException e) { return 100; }
    }

    private static String readField(int index, String def) {
        try {
            if (!Files.exists(FILE)) return def;
            String[] parts = Files.readString(FILE).trim().split("\\|");
            if (parts.length > index) return parts[index];
        } catch (Exception ignored) {}
        return def;
    }
}