package me.sentaihex.client.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cross-JVM cache: the companion agent (inside Minecraft) writes held item + heartbeat,
 * the launcher client reads it to gate XP bottle spam.
 */
public final class HeldItemBridge {

    private static final Path DIR =
            Paths.get(System.getProperty("user.home"), ".sentaihex");
    private static final Path ITEM_FILE = DIR.resolve("held-item.txt");
    private static final Path HEARTBEAT_FILE = DIR.resolve("agent-heartbeat.txt");

    /** Agent heartbeat must be fresh within this window. */
    private static final long STALE_MS = 2000;

    private HeldItemBridge() {}

    public static void writeHeldItemId(String itemId) {
        try {
            Files.createDirectories(DIR);
            Files.writeString(ITEM_FILE, itemId == null ? "" : itemId);
            Files.writeString(HEARTBEAT_FILE, Long.toString(System.currentTimeMillis()));
        } catch (Exception ignored) {}
    }

    public static String readHeldItemId() {
        try {
            if (!Files.exists(ITEM_FILE)) return "";
            return Files.readString(ITEM_FILE).trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isAgentActive() {
        try {
            if (!Files.exists(HEARTBEAT_FILE)) return false;
            long ts = Long.parseLong(Files.readString(HEARTBEAT_FILE).trim());
            return System.currentTimeMillis() - ts <= STALE_MS;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isHoldingExperienceBottle() {
        if (!isAgentActive()) return false;
        String id = readHeldItemId().toLowerCase();
        return id.contains("experience_bottle");
    }

    /** Agent linked but item detection not ready yet (world loading, etc.). */
    public static boolean isAgentInitializing() {
        if (!isAgentActive()) return false;
        String id = readHeldItemId();
        return id.isEmpty() || id.equals("init");
    }
}
