package me.sentaihex.client.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Diagnostic tool for troubleshooting TriggerBot file read/write issues.
 * Use: -Dtriggerbot.debug to enable verbose logging in TriggerBotBridge.
 *       java -Dtriggerbot.debug -Dtriggerbot.diagnose ...
 */
public class TriggerBotDebugger {

    private static final Path DIR = Paths.get(System.getProperty("user.home"), ".sentaihex");
    private static final Path FILE = DIR.resolve("triggerbot.txt");

    public static void diagnose() {
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║          TriggerBot Diagnostics                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        
        System.out.println("\n[1] File Location:");
        System.out.println("    Path: " + FILE.toAbsolutePath());
        System.out.println("    Exists: " + Files.exists(FILE));
        
        if (Files.exists(FILE)) {
            try {
                long fileSize = Files.size(FILE);
                System.out.println("    Size: " + fileSize + " bytes");
                
                // Read raw bytes
                byte[] bytes = Files.readAllBytes(FILE);
                System.out.println("\n[2] Raw Bytes (hex):");
                System.out.print("    ");
                for (byte b : bytes) {
                    System.out.printf("%02X ", b);
                }
                System.out.println();
                
                // Read as UTF-8
                String contentUTF8 = Files.readString(FILE, StandardCharsets.UTF_8);
                System.out.println("\n[3] Content (UTF-8):");
                System.out.println("    Length: " + contentUTF8.length() + " chars");
                System.out.println("    Raw: " + escapeString(contentUTF8));
                System.out.println("    Display: [" + contentUTF8 + "]");
                
                // Trimmed content
                String trimmed = contentUTF8.trim();
                System.out.println("\n[4] After trim():");
                System.out.println("    Length: " + trimmed.length() + " chars");
                System.out.println("    Raw: " + escapeString(trimmed));
                
                // Split analysis
                String[] parts = trimmed.split("\\|");
                System.out.println("\n[5] After split('|'):");
                System.out.println("    Parts count: " + parts.length);
                for (int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    String trimmedPart = part.trim();
                    System.out.println("    Part[" + i + "]: raw=" + escapeString(part) +
                            ", trimmed=" + escapeString(trimmedPart) +
                            ", length=" + part.length() + " -> " + trimmedPart.length());
                }
                
                // Parse test
                System.out.println("\n[6] Parsing Test:");
                if (parts.length >= 1) {
                    String enabledStr = parts[0].trim();
                    boolean enabled = "true".equalsIgnoreCase(enabledStr);
                    System.out.println("    enabled: '" + enabledStr + "' -> " + enabled);
                }
                if (parts.length >= 2) {
                    String slotStr = parts[1].trim();
                    try {
                        int slot = Integer.parseInt(slotStr);
                        System.out.println("    slot: '" + slotStr + "' -> " + slot);
                    } catch (Exception e) {
                        System.out.println("    slot: '" + slotStr + "' -> PARSE ERROR: " + e.getMessage());
                    }
                }
                if (parts.length >= 3) {
                    String delayStr = parts[2].trim();
                    try {
                        int delay = Integer.parseInt(delayStr);
                        System.out.println("    delay: '" + delayStr + "' -> " + delay);
                    } catch (Exception e) {
                        System.out.println("    delay: '" + delayStr + "' -> PARSE ERROR: " + e.getMessage());
                    }
                }
                
                // Bridge test
                System.out.println("\n[7] TriggerBotBridge Result:");
                System.out.println("    isEnabled(): " + TriggerBotBridge.isEnabled());
                System.out.println("    getWeaponSlot(): " + TriggerBotBridge.getWeaponSlot());
                System.out.println("    getDelayMs(): " + TriggerBotBridge.getDelayMs());
                
            } catch (Exception e) {
                System.err.println("    ERROR: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("    WARNING: File does not exist");
            System.out.println("\n[Test] Writing test config...");
            try {
                TriggerBotBridge.writeConfig(true, -1, 100);
                System.out.println("    ✓ Test write completed, please run diagnose again");
            } catch (Exception e) {
                System.err.println("    ✗ Test write failed: " + e.getMessage());
            }
        }
        
        System.out.println("\n[8] Environment:");
        System.out.println("    OS: " + System.getProperty("os.name"));
        System.out.println("    Java: " + System.getProperty("java.version"));
        System.out.println("    File.encoding: " + System.getProperty("file.encoding"));
        System.out.println("    Line separator: " + escapeString(System.getProperty("line.separator")));
        
        System.out.println("\n");
    }

    private static String escapeString(String s) {
        if (s == null) return "null";
        return s.replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replace("\0", "\\0");
    }

    public static void main(String[] args) {
        diagnose();
    }
}
