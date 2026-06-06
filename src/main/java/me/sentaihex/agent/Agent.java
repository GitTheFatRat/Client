package me.sentaihex.agent;

import me.sentaihex.client.util.HeldItemBridge;
import me.sentaihex.client.util.MinecraftAccess;
import me.sentaihex.client.util.TriggerBotBridge;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Companion agent inside the Minecraft JVM.
 * Handles:
 *   1. Held-item polling (XP bottle gating)
 *   2. TriggerBot attack execution
 */
public class Agent {

    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[SentaiHex] Companion agent attached to Minecraft JVM");
        HeldItemBridge.writeHeldItemId("init");

        Thread thread = new Thread(() -> {
            try {
                // Wait for Minecraft to be ready
                for (int attempt = 0; attempt < 120; attempt++) {
                    Thread.sleep(250);
                    if (MinecraftAccess.initMc(inst)) {
                        System.out.println("[SentaiHex] MC ready — starting held-item + triggerbot loops");
                        startLoops();
                        return;
                    }
                }
                System.out.println("[SentaiHex] Could not init MC — agent unavailable");
                HeldItemBridge.writeHeldItemId("");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[SentaiHex] Agent error: " + e.getMessage());
            }
        }, "SentaiHex-Agent");

        thread.setDaemon(true);
        thread.start();
    }

    public static void premain(String args, Instrumentation inst) {
        agentmain(args, inst);
    }

    // -------------------------------------------------------------------------
    // Start both loops on separate threads
    // -------------------------------------------------------------------------
    private static void startLoops() {
        // Loop 1: held item polling (existing)
        Thread heldItem = new Thread(() -> {
            try { pollHeldItemLoop(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "SentaiHex-HeldItem");
        heldItem.setDaemon(true);
        heldItem.start();

        // Loop 2: triggerbot
        Thread triggerBot = new Thread(() -> {
            try { triggerBotLoop(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "SentaiHex-TriggerBot");
        triggerBot.setDaemon(true);
        triggerBot.start();
    }

    private static String readTriggerBotFile() {
        try {
            java.nio.file.Path p = java.nio.file.Paths.get(
                    System.getProperty("user.home"), ".sentaihex", "triggerbot.txt");
            if (!p.toFile().exists()) return "MISSING";
            return java.nio.file.Files.readString(p);
        } catch (Exception e) { return "ERROR:" + e.getMessage(); }
    }

    // -------------------------------------------------------------------------
    // Loop 1: held item polling (unchanged)
    // -------------------------------------------------------------------------
    private static void pollHeldItemLoop() throws InterruptedException {
        while (true) {
            String id = MinecraftAccess.getHeldItemRegistryId();
            HeldItemBridge.writeHeldItemId(id);
            Thread.sleep(50);
        }
    }

    // -------------------------------------------------------------------------
    // Loop 2: TriggerBot — runs inside Minecraft JVM, has full MC access
    // -------------------------------------------------------------------------
    private static void triggerBotLoop() throws InterruptedException {
        System.out.println("[SentaiHex] TriggerBot loop started");
        long lastAttack  = 0;
        long lastDebugMs = 0;

        while (true) {
            Thread.sleep(10);

            long now   = System.currentTimeMillis();
            boolean debug = now - lastDebugMs > 2000;
            if (debug) lastDebugMs = now;

            boolean enabled = TriggerBotBridge.isEnabled();
            if (debug) System.out.println("[TriggerBot] enabled=" + enabled
                    + " file=" + java.nio.file.Paths.get(System.getProperty("user.home"), ".sentaihex", "triggerbot.txt").toFile().exists()
                    + " content=" + readTriggerBotFile());

            if (!enabled) continue;

            int  delay  = TriggerBotBridge.getDelayMs();
            int  jitter = ThreadLocalRandom.current().nextInt(-15, 16);
            if (now - lastAttack < delay + jitter) continue;

            if (isScreenOpen())              { if (debug) System.out.println("[TriggerBot] blocked: screen open"); continue; }

            int slotFilter = TriggerBotBridge.getWeaponSlot();
            if (slotFilter != -1 && getSelectedSlot() != slotFilter)
            { if (debug) System.out.println("[TriggerBot] blocked: wrong slot (need=" + slotFilter + " cur=" + getSelectedSlot() + ")"); continue; }

            if (!isCooldownReady())          { if (debug) System.out.println("[TriggerBot] blocked: cooldown not ready"); continue; }

            if (!MinecraftAccess.isLookingAtEntity())
            { if (debug) System.out.println("[TriggerBot] blocked: not looking at entity"); continue; }

            Object target = MinecraftAccess.getTargetEntity();
            if (target == null)              { if (debug) System.out.println("[TriggerBot] blocked: target null"); continue; }

            if (!isAlive(target))            { if (debug) System.out.println("[TriggerBot] blocked: target not alive"); continue; }

            if (!isInRange(target))          { if (debug) System.out.println("[TriggerBot] blocked: out of range"); continue; }

            boolean hit = MinecraftAccess.attackTargetEntity();
            if (hit) {
                lastAttack = System.currentTimeMillis();
                System.out.println("[TriggerBot] hit: " + target.getClass().getSimpleName());
            } else {
                if (debug) System.out.println("[TriggerBot] attack returned false");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers — all run inside Minecraft JVM so reflection works
    // -------------------------------------------------------------------------

    private static boolean isScreenOpen() {
        try {
            Object mc = getMc();
            if (mc == null) return false;
            for (String name : new String[]{"currentScreen", "screen", "field_1755"}) {
                try {
                    Field f = mc.getClass().getDeclaredField(name);
                    f.setAccessible(true);
                    return f.get(mc) != null;
                } catch (NoSuchFieldException ignored) {}
            }
            for (Field f : mc.getClass().getDeclaredFields()) {
                String t = f.getType().getName();
                if (t.contains("Screen") || t.contains("class_437")) {
                    f.setAccessible(true);
                    return f.get(mc) != null;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static int getSelectedSlot() {
        try {
            Object player = getPlayer();
            if (player == null) return -1;
            for (String invName : new String[]{"getInventory", "method_31548"}) {
                try {
                    Method m = player.getClass().getMethod(invName);
                    m.setAccessible(true);
                    Object inv = m.invoke(player);
                    if (inv == null) continue;
                    for (String slotName : new String[]{"selectedSlot", "selected", "field_7545"}) {
                        try {
                            Field f = inv.getClass().getDeclaredField(slotName);
                            f.setAccessible(true);
                            return (int) f.get(inv);
                        } catch (NoSuchFieldException ignored) {}
                    }
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private static boolean isCooldownReady() {
        try {
            Object player = getPlayer();
            if (player == null) return true;
            for (String name : new String[]{"getAttackStrengthScale", "method_6039"}) {
                try {
                    Method m = player.getClass().getMethod(name, float.class);
                    m.setAccessible(true);
                    Object r = m.invoke(player, 0.0f);
                    if (r instanceof Number n) return n.floatValue() >= 0.9f;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception ignored) {}
        return true;
    }

    private static boolean isAlive(Object entity) {
        try {
            for (String name : new String[]{"isAlive", "method_5805"}) {
                try {
                    Method m = entity.getClass().getMethod(name);
                    m.setAccessible(true);
                    Object r = m.invoke(entity);
                    if (r instanceof Boolean b) return b;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception ignored) {}
        return true;
    }

    private static boolean isInRange(Object entity) {
        try {
            Object player = getPlayer();
            if (player == null) return true;
            double px = getDouble(player, "getX", "method_23317");
            double py = getDouble(player, "getY", "method_23318");
            double pz = getDouble(player, "getZ", "method_23321");
            double ex = getDouble(entity, "getX", "method_23317");
            double ey = getDouble(entity, "getY", "method_23318");
            double ez = getDouble(entity, "getZ", "method_23321");
            double dx = px - ex, dy = py - ey, dz = pz - ez;
            return (dx * dx + dy * dy + dz * dz) <= 9.0;
        } catch (Exception ignored) {}
        return true;
    }

    private static double getDouble(Object obj, String... names) {
        for (String name : names) {
            try {
                Method m = obj.getClass().getMethod(name);
                m.setAccessible(true);
                Object r = m.invoke(obj);
                if (r instanceof Number n) return n.doubleValue();
            } catch (Exception ignored) {}
        }
        return 0.0;
    }

    private static Object getMc() {
        // MinecraftAccess already initialized MC, reuse its class loader approach
        try {
            ClassLoader cl = null;
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if ("Render thread".equals(t.getName())) {
                    cl = t.getContextClassLoader();
                    break;
                }
            }
            if (cl == null) return null;
            for (String name : new String[]{
                    "net.minecraft.client.MinecraftClient",
                    "net.minecraft.client.Minecraft",
                    "net.minecraft.class_310"}) {
                try {
                    Class<?> c = Class.forName(name, true, cl);
                    for (Field f : c.getDeclaredFields()) {
                        f.setAccessible(true);
                        if (f.getType().equals(c)
                                && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                            Object inst = f.get(null);
                            if (inst != null) return inst;
                        }
                    }
                } catch (ClassNotFoundException ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Object getPlayer() {
        try {
            Object mc = getMc();
            if (mc == null) return null;
            for (String name : new String[]{"player", "thePlayer", "field_1724"}) {
                try {
                    Field f = mc.getClass().getDeclaredField(name);
                    f.setAccessible(true);
                    return f.get(mc);
                } catch (NoSuchFieldException ignored) {}
            }
            for (Field f : mc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                String t = f.getType().getName();
                if (t.contains("LocalPlayer") || t.contains("ClientPlayer") || t.contains("class_746"))
                    return f.get(mc);
            }
        } catch (Exception ignored) {}
        return null;
    }
}