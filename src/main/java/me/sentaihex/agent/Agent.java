package me.sentaihex.agent;

import me.sentaihex.client.util.HeldItemBridge;
import me.sentaihex.client.util.MinecraftAccess;
import me.sentaihex.client.util.TriggerBotBridge;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;

public class Agent {
    private static final long lastGuiToggle = 0;

    // Cached fields/methods for performance
    private static Object mcCached = null;
    private static final Object playerCached = null;
    private static Method getAttackStrengthMethod = null;
    private static Method isAliveMethod = null;
    private static Field  inventoryField = null;
    private static Field  selectedSlotField = null;

    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[SentaiHex] Companion agent attached to Minecraft JVM");
        HeldItemBridge.writeHeldItemId("init");

        // Fix HeadlessException TRƯỚC KHI khởi động thread
        // Modrinth/Prism set java.awt.headless=true — phải reset ngay tại đây
        // trước khi Launcher gọi SentaiHex.start() và tạo ClickGUI
        fixHeadless(inst);

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

    /**
     * Reset java.awt.headless=false và clear cached GraphicsEnvironment/Toolkit
     * để Swing có thể tạo JFrame ngay cả khi launcher đã set headless=true.
     * Thử nhiều cách vì Modrinth lock module java.desktop.
     */
    private static void fixHeadless(Instrumentation inst) {
        System.setProperty("java.awt.headless", "false");

        // Attempt 1: redefineModule để mở java.desktop (dùng Instrumentation)
        if (inst != null) {
            try {
                Class<?> toolkitClass = Class.forName("java.awt.Toolkit");
                Module desktopModule = toolkitClass.getModule();
                Module thisModule = Agent.class.getModule();
                if (desktopModule.isNamed()) {
                    java.util.Map<String, java.util.Set<Module>> extraOpens = new java.util.HashMap<>();
                    for (String pkg : desktopModule.getPackages()) {
                        extraOpens.put(pkg, java.util.Collections.singleton(thisModule));
                    }
                    inst.redefineModule(
                            desktopModule,
                            java.util.Collections.emptySet(),
                            java.util.Collections.emptyMap(),
                            extraOpens,
                            java.util.Collections.emptySet(),
                            java.util.Collections.emptyMap()
                    );
                    System.out.println("[SentaiHex] java.desktop module opened via Instrumentation");
                }
            } catch (Exception e) {
                System.out.println("[SentaiHex] redefineModule skipped: " + e.getMessage());
            }
        }

        // Attempt 2: reset Toolkit.toolkit field
        try {
            Class<?> toolkitClass = Class.forName("java.awt.Toolkit");
            Field field = toolkitClass.getDeclaredField("toolkit");
            field.setAccessible(true);
            field.set(null, null);
            System.out.println("[SentaiHex] Toolkit reset OK");
        } catch (Exception e) {
            System.out.println("[SentaiHex] Toolkit reset skipped: " + e.getMessage());
        }

        // Attempt 3: reset GraphicsEnvironment.headless + localGE
        try {
            Class<?> geClass = Class.forName("java.awt.GraphicsEnvironment");
            for (Field f : geClass.getDeclaredFields()) {
                f.setAccessible(true);
                String name = f.getName();
                if ((name.contains("headless") || name.contains("Headless"))
                        && f.getType() == boolean.class) {
                    f.set(null, false);
                    System.out.println("[SentaiHex] Reset GE field: " + name);
                } else if (name.contains("headless") && f.getType() == Boolean.class) {
                    f.set(null, null); // null = unset, will re-detect
                    System.out.println("[SentaiHex] Cleared GE Boolean field: " + name);
                } else if (f.getType().getName().contains("GraphicsEnvironment")) {
                    f.set(null, null);
                    System.out.println("[SentaiHex] Cleared localGE field: " + name);
                }
            }
        } catch (Exception e) {
            System.out.println("[SentaiHex] GE reset skipped: " + e.getMessage());
        }

        // Attempt 4: Module.implAddOpens fallback
        try {
            Class<?> toolkitClass = Class.forName("java.awt.Toolkit");
            Module desktopModule = toolkitClass.getModule();
            Module thisModule = Agent.class.getModule();
            if (desktopModule.isNamed() && !desktopModule.isOpen("java.awt", thisModule)) {
                Method implAddOpens = Module.class.getDeclaredMethod("implAddOpens", String.class, Module.class);
                implAddOpens.setAccessible(true);
                for (String pkg : desktopModule.getPackages()) {
                    try { implAddOpens.invoke(desktopModule, pkg, thisModule); }
                    catch (Exception ignored) {}
                }
                System.out.println("[SentaiHex] java.desktop opened via implAddOpens");
                // Try toolkit reset again after module opened
                Field field = toolkitClass.getDeclaredField("toolkit");
                field.setAccessible(true);
                field.set(null, null);
                System.out.println("[SentaiHex] Toolkit reset OK (after implAddOpens)");
            }
        } catch (Exception e) {
            System.out.println("[SentaiHex] implAddOpens skipped: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Start both loops on separate threads
    // -------------------------------------------------------------------------
    private static void startLoops() {
        Thread heldItem = new Thread(() -> {
            try { pollHeldItemLoop(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "SentaiHex-HeldItem");
        heldItem.setDaemon(true);
        heldItem.start();

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

    private static void pollHeldItemLoop() throws InterruptedException {
        while (true) {
            String id = MinecraftAccess.getHeldItemRegistryId();
            HeldItemBridge.writeHeldItemId(id);
            Thread.sleep(50);
        }
    }

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

            if (inventoryField == null) {
                for (String invName : new String[]{"getInventory", "method_31548", "inventory", "field_7514"}) {
                    try {
                        // Try method first
                        try {
                            Method m = player.getClass().getMethod(invName);
                            m.setAccessible(true);
                            Object inv = m.invoke(player);
                            if (inv != null) {
                                // Find field in inventory class
                                for (String slotName : new String[]{"selectedSlot", "selected", "field_7545"}) {
                                    try {
                                        Field f = inv.getClass().getDeclaredField(slotName);
                                        f.setAccessible(true);
                                        selectedSlotField = f;
                                        // Cache the inventory field or method? Let's just use it once.
                                        return (int) f.get(inv);
                                    } catch (NoSuchFieldException ignored) {}
                                }
                            }
                        } catch (NoSuchMethodException e) {
                            // Try field
                            Field fInv = player.getClass().getDeclaredField(invName);
                            fInv.setAccessible(true);
                            Object inv = fInv.get(player);
                            if (inv != null) {
                                for (String slotName : new String[]{"selectedSlot", "selected", "field_7545"}) {
                                    try {
                                        Field f = inv.getClass().getDeclaredField(slotName);
                                        f.setAccessible(true);
                                        selectedSlotField = f;
                                        return (int) f.get(inv);
                                    } catch (NoSuchFieldException ignored) {}
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            } else if (selectedSlotField != null) {
                // Cached field - try using it
                try {
                    Object player = getPlayer();
                    if (player != null) {
                        for (String invName : new String[]{"getInventory", "method_31548"}) {
                            try {
                                Method m = player.getClass().getMethod(invName);
                                m.setAccessible(true);
                                Object inv = m.invoke(player);
                                if (inv != null) {
                                    return (int) selectedSlotField.get(inv);
                                }
                            } catch (NoSuchMethodException ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // Fallback to the original logic if caching is tricky
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

            if (getAttackStrengthMethod == null) {
                for (String name : new String[]{"getAttackStrengthScale", "method_6039"}) {
                    try {
                        Method m = player.getClass().getMethod(name, float.class);
                        m.setAccessible(true);
                        getAttackStrengthMethod = m;
                        break;
                    } catch (NoSuchMethodException ignored) {}
                }
            }

            if (getAttackStrengthMethod != null) {
                Object r = getAttackStrengthMethod.invoke(player, 0.0f);
                if (r instanceof Number n) return n.floatValue() >= 0.95f;
            }
        } catch (Exception ignored) {}
        return true;
    }

    private static boolean isAlive(Object entity) {
        try {
            if (isAliveMethod == null) {
                for (String name : new String[]{"isAlive", "method_5805"}) {
                    try {
                        Method m = entity.getClass().getMethod(name);
                        m.setAccessible(true);
                        isAliveMethod = m;
                        break;
                    } catch (NoSuchMethodException ignored) {}
                }
            }

            if (isAliveMethod != null) {
                Object r = isAliveMethod.invoke(entity);
                if (r instanceof Boolean b) return b;
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
        if (mcCached != null) return mcCached;
        try {
            ClassLoader cl = null;
            // 1. Check render thread
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if ("Render thread".equals(t.getName())) {
                    cl = t.getContextClassLoader();
                    break;
                }
            }
            // 2. Fallback: check all threads for one that can see MinecraftClient
            if (cl == null) {
                for (Thread t : Thread.getAllStackTraces().keySet()) {
                    ClassLoader tcl = t.getContextClassLoader();
                    if (tcl == null) continue;
                    try {
                        Class.forName("net.minecraft.class_310", false, tcl);
                        cl = tcl;
                        break;
                    } catch (Exception ignored) {}
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
                            if (inst != null) {
                                mcCached = inst;
                                return inst;
                            }
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
}