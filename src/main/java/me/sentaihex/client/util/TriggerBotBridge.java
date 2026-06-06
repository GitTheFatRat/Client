package me.sentaihex.client.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public final class TriggerBotBridge {

    private static final Path DIR = Paths.get(System.getProperty("user.home"), ".sentaihex");
    private static final Path FILE = DIR.resolve("triggerbot.txt");

    private static boolean lastEnabled = false;
    private static int lastWeaponSlot = -1;
    private static int lastDelay = 50;
    private static long lastReadTime = 0;
    private static final long CACHE_MS = 100;

    // Platform detection for logging
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    private TriggerBotBridge() {}

    /**
     * Write TriggerBot config to file atomically with UTF-8 encoding.
     * Format: enabled|weaponSlot|delayMs
     * Platform: Windows-compatible (handles CRLF properly)
     */
    public static void writeConfig(boolean enabled, int weaponSlot, int delayMs) {
        try {
            Files.createDirectories(DIR);
            String config = enabled + "|" + weaponSlot + "|" + delayMs;
            
            // Write atomically with UTF-8 (no BOM), truncate existing file
            Files.writeString(FILE, config, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            
            System.out.println("[TriggerBotBridge] ✓ Config written: enabled=" + enabled + 
                    ", slot=" + weaponSlot + ", delay=" + delayMs + 
                    " (path=" + FILE.toAbsolutePath() + ")");
            
            // Force OS flush on Windows to prevent race conditions
            if (IS_WINDOWS) {
                Thread.sleep(10);
            }
        } catch (Exception e) {
            System.err.println("[TriggerBotBridge] ✗ Write failed: " + e.getClass().getSimpleName() + 
                    " - " + e.getMessage() + " (path=" + FILE.toAbsolutePath() + ")");
            e.printStackTrace();
        }
    }

    /**
     * Check if TriggerBot is enabled (cached).
     * @return true if file says "true", false otherwise
     */
    public static boolean isEnabled() {
        refreshIfNeeded();
        return lastEnabled;
    }

    /**
     * Get weapon slot (cached).
     * @return weapon slot or -1 if not found
     */
    public static int getWeaponSlot() {
        refreshIfNeeded();
        return lastWeaponSlot;
    }

    /**
     * Get delay in milliseconds (cached, minimum 10ms).
     * @return delay or 50ms default
     */
    public static int getDelayMs() {
        refreshIfNeeded();
        return Math.max(10, lastDelay);
    }

    /**
     * Refresh cache if needed (every 100ms).
     * Handles Windows CRLF, UTF-8 encoding, and whitespace properly.
     */
    private static void refreshIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastReadTime < CACHE_MS) {
            return; // Use cache
        }
        lastReadTime = now;

        try {
            if (!Files.exists(FILE)) {
                System.out.println("[TriggerBotBridge] ⚠ Config file NOT FOUND: " + FILE.toAbsolutePath());
                System.out.println("[TriggerBotBridge] Create file with format: true|slot|delay (e.g., true|0|50)");
                logDebug("File not found, using defaults");
                lastEnabled = false;
                lastWeaponSlot = -1;
                lastDelay = 50;
                return;
            }

            // Read with UTF-8 explicitly
            String content = Files.readString(FILE, StandardCharsets.UTF_8);
            
            // Debug: log raw content with escape codes visible
            logDebug("Raw content (length=" + content.length() + "): " + 
                    escapeString(content));

            // Trim full string first (remove leading/trailing whitespace and BOM)
            content = content.trim();
            
            if (content.isEmpty()) {
                logDebug("Content is empty after trim");
                lastEnabled = false;
                lastWeaponSlot = -1;
                lastDelay = 50;
                return;
            }

            // Split and trim each part (critical for Windows CRLF handling)
            String[] parts = content.split("\\|");
            logDebug("After split: " + parts.length + " parts");

            // Parse enabled (part 0)
            if (parts.length >= 1) {
                String enabledStr = parts[0].trim(); // Trim each part!
                logDebug("Part[0]: '" + escapeString(enabledStr) + "'");
                lastEnabled = "true".equalsIgnoreCase(enabledStr);
            } else {
                lastEnabled = false;
            }

            // Parse weaponSlot (part 1)
            if (parts.length >= 2) {
                try {
                    String slotStr = parts[1].trim();
                    logDebug("Part[1]: '" + escapeString(slotStr) + "'");
                    lastWeaponSlot = Integer.parseInt(slotStr);
                } catch (NumberFormatException e) {
                    logDebug("Failed to parse slot: " + e.getMessage());
                    lastWeaponSlot = -1;
                }
            } else {
                lastWeaponSlot = -1;
            }

            // Parse delay (part 2)
            if (parts.length >= 3) {
                try {
                    String delayStr = parts[2].trim();
                    logDebug("Part[2]: '" + escapeString(delayStr) + "'");
                    lastDelay = Integer.parseInt(delayStr);
                } catch (NumberFormatException e) {
                    logDebug("Failed to parse delay: " + e.getMessage());
                    lastDelay = 50;
                }
            } else {
                lastDelay = 50;
            }
            
            logDebug("Parsed: enabled=" + lastEnabled + ", slot=" + lastWeaponSlot + 
                    ", delay=" + lastDelay);
            
        } catch (Exception e) {
            System.err.println("[TriggerBotBridge] ✗ Read error: " + e.getClass().getSimpleName() + 
                    " - " + e.getMessage() + " (path=" + FILE.toAbsolutePath() + ")");
            e.printStackTrace();
            lastEnabled = false;
            lastWeaponSlot = -1;
            lastDelay = 50;
        }
    }

    /**
     * Log debug message (show hidden chars for troubleshooting).
     */
    private static void logDebug(String msg) {
        if (System.getProperty("triggerbot.debug") != null) {
            System.out.println("[TriggerBotBridge] DEBUG: " + msg);
        }
    }

    /**
     * Escape string to show hidden characters (\\n, \\r, etc).
     */
    private static String escapeString(String s) {
        if (s == null) return "null";
        return s.replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\0", "\\0");
    }
}