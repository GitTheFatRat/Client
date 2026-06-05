package me.sentaihex.launcher;

import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 * Detects the Minecraft <em>game</em> JVM, not launcher apps (Prism, Modrinth/Theseus, etc.).
 */
public final class ProcessDetector {

    private ProcessDetector() {}

    public enum ProcessKind {
        MINECRAFT_GAME,
        OTHER
    }

    public record DetectedProcess(VirtualMachineDescriptor descriptor, ProcessKind kind, String label) {}

    public static DetectedProcess classify(VirtualMachineDescriptor vmd) {
        String display = vmd.displayName();
        String lower = display.toLowerCase();

        if (isSentaiHexProcess(lower)) {
            return new DetectedProcess(vmd, ProcessKind.OTHER, truncate(display));
        }

        if (isLauncherOnlyProcess(lower)) {
            return new DetectedProcess(vmd, ProcessKind.OTHER, truncate(display));
        }

        if (isMinecraftGameProcess(lower)) {
            String tag = launcherTag(lower);
            String label = tag.isEmpty()
                    ? truncate(display)
                    : "[" + tag + "] " + truncate(display);
            return new DetectedProcess(vmd, ProcessKind.MINECRAFT_GAME, label);
        }

        return new DetectedProcess(vmd, ProcessKind.OTHER, truncate(display));
    }

    public static boolean isMinecraftGame(VirtualMachineDescriptor vmd) {
        return classify(vmd).kind() == ProcessKind.MINECRAFT_GAME;
    }

    private static boolean isSentaiHexProcess(String lower) {
        return lower.contains("sentaihex")
                || lower.contains("me.sentaihex");
    }

    /**
     * Launcher UIs that share a JVM with Java but are not the game itself.
     */
    private static boolean isLauncherOnlyProcess(String lower) {
        boolean hasGameMain = lower.contains("knotclient")
                || lower.contains("net.minecraft.client.main")
                || lower.contains("modlauncher")
                || lower.contains("launchwrapper")
                || lower.contains("minecraftclient");

        if (hasGameMain) {
            return false;
        }

        return lower.contains("theseus")
                || lower.contains("modrinth")
                || lower.contains("prismlauncher")
                || lower.contains("org.prismlauncher")
                || lower.contains("multimc")
                || lower.contains("polymc")
                || lower.contains("com.modrinth");
    }

    private static boolean isMinecraftGameProcess(String lower) {
        // Vanilla / Fabric / Quilt / Forge game entry points
        if (lower.contains("knotclient")) return true;
        if (lower.contains("net.minecraft.client.main")) return true;
        if (lower.contains("net.minecraft.client.main.main")) return true;
        if (lower.contains("cpw.mods.modlauncher.launcher")) return true;
        if (lower.contains("modlauncher") && !lower.contains("prismlauncher")) return true;
        if (lower.contains("launchwrapper")) return true;
        if (lower.contains("net.minecraft.launchwrapper")) return true;
        if (lower.contains("com.mojang.minecraft")) return true;

        // Third-party clients
        if (lower.contains("lunar")
                || lower.contains("com.moonsworth")
                || lower.contains("lunarclient")) return true;
        if (lower.contains("feather") || lower.contains("gg.essential")) return true;
        if (lower.contains("badlion") || lower.contains("digitalingot")) return true;
        if (lower.contains("labymod")) return true;
        if (lower.contains("legacylauncher") || lower.contains("tlauncher")) return true;
        if (lower.contains("pvplegacy") || lower.contains("proxiedstart")) return true;

        // Generic fallback — must mention minecraft and not be a bare launcher
        if (lower.contains("net.minecraft")) return true;

        return false;
    }

    private static String launcherTag(String lower) {
        if (lower.contains("knotclient") || lower.contains("fabricmc")) return "Fabric";
        if (lower.contains("modlauncher")) return "Forge";
        if (lower.contains("feather")) return "Feather";
        if (lower.contains("lunar")) return "Lunar";
        if (lower.contains("badlion")) return "Badlion";
        if (lower.contains("labymod")) return "LabyMod";
        if (lower.contains("net.minecraft.client.main")) return "Vanilla";
        return "MC";
    }

    private static String truncate(String display) {
        if (display.length() <= 72) return display;
        return display.substring(0, 72) + "...";
    }
}
