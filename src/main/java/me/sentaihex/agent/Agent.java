package me.sentaihex.agent;

import me.sentaihex.client.util.HeldItemBridge;
import me.sentaihex.client.util.MinecraftAccess;
import me.sentaihex.client.util.TriggerBotBridge;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Agent {

    // Cached reflections
    private static final AtomicReference<Object> MC_CACHED = new AtomicReference<>(null);
    private static final AtomicReference<Object> PLAYER_CACHED = new AtomicReference<>(null);
    private static final AtomicReference<Object> INVENTORY_CACHED = new AtomicReference<>(null);
    private static final AtomicReference<Method> GET_SELECTED_SLOT_METHOD = new AtomicReference<>(null);
    private static final AtomicReference<Field> SELECTED_SLOT_FIELD = new AtomicReference<>(null);
    private static final AtomicReference<Method> GET_ATTACK_STRENGTH_METHOD = new AtomicReference<>(null);
    private static final AtomicReference<Method> IS_ALIVE_METHOD = new AtomicReference<>(null);
    private static final AtomicReference<Field> CURRENT_SCREEN_FIELD = new AtomicReference<>(null);
    private static final AtomicReference<Method> GET_X_METHOD = new AtomicReference<>(null);
    private static final AtomicReference<Method> GET_Y_METHOD = new AtomicReference<>(null);
    private static final AtomicReference<Method> GET_Z_METHOD = new AtomicReference<>(null);

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static final AtomicInteger LAST_KNOWN_SLOT = new AtomicInteger(-1);
    private static final AtomicInteger LAST_SCREEN_STATE = new AtomicInteger(0); // 0=unknown, 1=closed, 2=open
    private static long lastSlotCheckTime = 0;
    private static long lastScreenCheckTime = 0;
    private static long lastAttackTime = 0;
    private static long lastDebugTime = 0;

    private static final long SLOT_CHECK_INTERVAL_MS = 50;
    private static final long SCREEN_CHECK_INTERVAL_MS = 100;
    private static final long DEBUG_INTERVAL_MS = 3000;

    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[SentaiHex] Agent attaching to Minecraft JVM...");
        HeldItemBridge.writeHeldItemId("init");

        fixHeadless(inst);

        Thread agentThread = new Thread(() -> {
            try {
                for (int attempt = 0; attempt < 120; attempt++) {
                    Thread.sleep(250);
                    if (MinecraftAccess.initMc(inst)) {
                        System.out.println("[SentaiHex] Minecraft initialized, starting agent...");
                        initializeReflections();
                        startBackgroundTasks();
                        INITIALIZED.set(true);
                        System.out.println("[SentaiHex] Agent ready!");
                        return;
                    }
                }
                System.out.println("[SentaiHex] Failed to initialize Minecraft access");
                HeldItemBridge.writeHeldItemId("");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[SentaiHex] Agent error: " + e.getMessage());
                e.printStackTrace();
            }
        }, "SentaiHex-Agent");

        agentThread.setDaemon(true);
        agentThread.start();
    }

    public static void premain(String args, Instrumentation inst) {
        agentmain(args, inst);
    }

    private static void initializeReflections() {
        try {
            System.out.println("[SentaiHex] Starting reflection cache...");
            
            cacheMinecraftInstance();
            System.out.println("[SentaiHex] ✓ Step 1: Minecraft instance cached");
            
            cachePlayer();
            System.out.println("[SentaiHex] ✓ Step 2: Player cached");
            
            cacheInventoryAndSelectedSlot();
            System.out.println("[SentaiHex] ✓ Step 3: Inventory cached");
            
            cacheAttackStrength();
            System.out.println("[SentaiHex] ✓ Step 4: Attack strength cached");
            
            cacheIsAlive();
            System.out.println("[SentaiHex] ✓ Step 5: IsAlive cached");
            
            cacheScreenField();
            System.out.println("[SentaiHex] ✓ Step 6: Screen field cached");
            
            cachePositionMethods();
            System.out.println("[SentaiHex] ✓ Step 7: Position methods cached");
            
            System.out.println("[SentaiHex] ✅ Reflection cache COMPLETE");
        } catch (Exception e) {
            System.err.println("[SentaiHex] ❌ Reflection error at some step: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void cacheMinecraftInstance() {
        try {
            ClassLoader cl = findMinecraftClassLoader();
            if (cl == null) return;

            String[] classNames = {
                    "net.minecraft.client.MinecraftClient",
                    "net.minecraft.client.Minecraft",
                    "net.minecraft.class_310"
            };

            for (String className : classNames) {
                try {
                    Class<?> mcClass = Class.forName(className, true, cl);
                    for (Field f : mcClass.getDeclaredFields()) {
                        f.setAccessible(true);
                        if (f.getType().equals(mcClass) && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                            Object instance = f.get(null);
                            if (instance != null) {
                                MC_CACHED.set(instance);
                                System.out.println("[SentaiHex] Minecraft instance cached: " + f.getName());
                                return;
                            }
                        }
                    }
                } catch (ClassNotFoundException ignored) {}
            }
        } catch (Exception e) {
            System.err.println("[SentaiHex] Failed to cache Minecraft: " + e.getMessage());
        }
    }

    private static void cachePlayer() {
        try {
            Object mc = MC_CACHED.get();
            if (mc == null) return;

            String[] fieldNames = {"player", "thePlayer", "field_1724"};
            for (String name : fieldNames) {
                try {
                    Field f = mc.getClass().getDeclaredField(name);
                    f.setAccessible(true);
                    Object player = f.get(mc);
                    if (player != null) {
                        PLAYER_CACHED.set(player);
                        System.out.println("[SentaiHex] Player cached: " + name);
                        return;
                    }
                } catch (NoSuchFieldException ignored) {}
            }

            // Scan by type
            for (Field f : mc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                String typeName = f.getType().getName();
                if (typeName.contains("LocalPlayer") || typeName.contains("ClientPlayer") || typeName.contains("class_746")) {
                    Object player = f.get(mc);
                    if (player != null) {
                        PLAYER_CACHED.set(player);
                        System.out.println("[SentaiHex] Player cached by type: " + f.getName());
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SentaiHex] Failed to cache player: " + e.getMessage());
        }
    }

    private static void cacheInventoryAndSelectedSlot() {
        try {
            Object player = PLAYER_CACHED.get();
            if (player == null) return;
theme
            Object inventory = null;

            // Try to get inventory
            String[] invNames = {"getInventory", "method_31548", "inventory", "field_7514"};
            for (String name : invNames) {
                try {
                    Method m = player.getClass().getMethod(name);
                    m.setAccessible(true);
                    inventory = m.invoke(player);
                    if (inventory != null) {
                        System.out.println("[SentaiHex] Inventory via method: " + name);
                        break;
                    }
                } catch (NoSuchMethodException e) {
                    try {
                        Field f = player.getClass().getDeclaredField(name);
                        f.setAccessible(true);
                        inventory = f.get(player);
                        if (inventory != null) {
                            System.out.println("[SentaiHex] Inventory via field: " + name);
                            break;
                        }
                    } catch (NoSuchFieldException ignored) {}
                }
            }

            if (inventory == null) {
                System.out.println("[SentaiHex] Cannot find inventory");
                return;
            }

            INVENTORY_CACHED.set(inventory);

            // Try to get selected slot via method
            String[] slotMethodNames = {"getSelectedSlot", "method_31547"};
            for (String name : slotMethodNames) {
                try {
                    Method m = inventory.getClass().getMethod(name);
                    m.setAccessible(true);
                    GET_SELECTED_SLOT_METHOD.set(m);
                    System.out.println("[SentaiHex] Selected slot via method: " + name);
                    return;
                } catch (NoSuchMethodException ignored) {}
            }

            // Try via field
            String[] slotFieldNames = {"selectedSlot", "selected", "field_7545"};
            for (String name : slotFieldNames) {
                try {
                    Field f = inventory.getClass().getDeclaredField(name);
                    f.setAccessible(true);
                    SELECTED_SLOT_FIELD.set(f);
                    System.out.println("[SentaiHex] Selected slot via field: " + name);
                    return;
                } catch (NoSuchFieldException ignored) {}
            }

            // Scan all int fields
            for (Field f : inventory.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType() == int.class) {
                    String name = f.getName().toLowerCase();
                    if (name.contains("selected") || name.contains("current") || name.contains("slot")) {
                        SELECTED_SLOT_FIELD.set(f);
                        System.out.println("[SentaiHex] Selected slot via scan: " + f.getName());
                        return;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[SentaiHex] Failed to cache inventory: " + e.getMessage());
        }
    }

    private static void cacheAttackStrength() {
        try {
            Object player = PLAYER_CACHED.get();
            if (player == null) return;

            String[] names = {"getAttackStrengthScale", "method_6039"};
            for (String name : names) {
                try {
                    Method m = player.getClass().getMethod(name, float.class);
                    m.setAccessible(true);
                    GET_ATTACK_STRENGTH_METHOD.set(m);
                    System.out.println("[SentaiHex] Attack strength cached: " + name);
                    return;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception e) {
            System.err.println("[SentaiHex] Failed to cache attack strength: " + e.getMessage());
        }
    }

    private static void cacheIsAlive() {
        try {
            Object player = PLAYER_CACHED.get();
            if (player == null) return;

            String[] names = {"isAlive", "method_5805"};
            for (String name : names) {
                try {
                    Method m = player.getClass().getMethod(name);
                    m.setAccessible(true);
                    IS_ALIVE_METHOD.set(m);
                    System.out.println("[SentaiHex] IsAlive cached: " + name);
                    return;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception e) {
            System.err.println("[SentaiHex] Failed to cache isAlive: " + e.getMessage());
        }
    }

    private static void cacheScreenField() {
        try {
            Object mc = MC_CACHED.get();
            if (mc == null) {
                System.err.println("[SentaiHex] ⚠ MC_CACHED is null, cannot cache screen field!");
                return;
            }

            // Comprehensive list of field names for different Minecraft versions
            // Including 1.21.11 variants
            String[] names = {
                "currentScreen",    // 1.12-1.20
                "screen",          // 1.20.2+, 1.21+
                "field_1755",      // Older MCP mapping
                "f_91183_",        // Yarn 1.20
                "c",               // Intermediary/Some versions
                "d",               // Some Forge versions
                "field_3860",      // Alternative MCP mapping
                "field_311"        // Another variant
            };
            
            System.out.println("[SentaiHex] Trying to find screen field (" + names.length + " names to try)...");
            
            for (String name : names) {
                try {
                    Field f = mc.getClass().getDeclaredField(name);
                    f.setAccessible(true);
                    CURRENT_SCREEN_FIELD.set(f);
                    System.out.println("[SentaiHex] ✓ Screen field FOUND and cached: " + name);
                    return;
                } catch (NoSuchFieldException ignored) {
                    if (System.getProperty("triggerbot.verbose.screen") != null) {
                        System.out.println("[SentaiHex] Screen field not found: " + name);
                    }
                }
            }
            
            System.err.println("[SentaiHex] ⚠⚠⚠ FAILED to find screen field by name! Listing ALL available fields in MC class:");
            System.err.println("[SentaiHex] Minecraft class: " + mc.getClass().getName());
            System.err.println("[SentaiHex] Available fields:");
            for (Field f : mc.getClass().getDeclaredFields()) {
                System.err.println("  - " + f.getName() + " (" + f.getType().getSimpleName() + ")");
            }
        } catch (Exception e) {
            System.err.println("[SentaiHex] ❌ Exception in cacheScreenField: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void cachePositionMethods() {
        try {
            Object player = PLAYER_CACHED.get();
            if (player == null) return;

            String[] xNames = {"getX", "method_23317"};
            for (String name : xNames) {
                try {
                    Method m = player.getClass().getMethod(name);
                    m.setAccessible(true);
                    GET_X_METHOD.set(m);
                    break;
                } catch (NoSuchMethodException ignored) {}
            }

            String[] yNames = {"getY", "method_23318"};
            for (String name : yNames) {
                try {
                    Method m = player.getClass().getMethod(name);
                    m.setAccessible(true);
                    GET_Y_METHOD.set(m);
                    break;
                } catch (NoSuchMethodException ignored) {}
            }

            String[] zNames = {"getZ", "method_23321"};
            for (String name : zNames) {
                try {
                    Method m = player.getClass().getMethod(name);
                    m.setAccessible(true);
                    GET_Z_METHOD.set(m);
                    break;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception e) {
            System.err.println("[SentaiHex] Failed to cache position: " + e.getMessage());
        }
    }

    private static void fixHeadless(Instrumentation inst) {
        System.setProperty("java.awt.headless", "false");

        if (inst != null) {
            try {
                Class<?> toolkitClass = Class.forName("java.awt.Toolkit");
                Module desktopModule = toolkitClass.getModule();
                Module thisModule = Agent.class.getModule();

                if (desktopModule.isNamed()) {
                    Map<String, Set<Module>> extraOpens = new HashMap<>();
                    for (String pkg : desktopModule.getPackages()) {
                        extraOpens.put(pkg, Set.of(thisModule));
                    }
                    inst.redefineModule(desktopModule, Set.of(), Map.of(), extraOpens, Set.of(), Map.of());
                    System.out.println("[SentaiHex] java.desktop module opened");
                }
            } catch (Exception e) {
                System.out.println("[SentaiHex] Could not open java.desktop: " + e.getMessage());
            }
        }

        try {
            Class<?> toolkitClass = Class.forName("java.awt.Toolkit");
            Field toolkitField = toolkitClass.getDeclaredField("toolkit");
            toolkitField.setAccessible(true);
            toolkitField.set(null, null);
        } catch (Exception ignored) {}

        try {
            Class<?> geClass = Class.forName("java.awt.GraphicsEnvironment");
            for (Field f : geClass.getDeclaredFields()) {
                f.setAccessible(true);
                String name = f.getName();
                if (name.contains("headless") && f.getType() == boolean.class) {
                    f.set(null, false);
                } else if (name.contains("headless") && f.getType() == Boolean.class) {
                    f.set(null, null);
                } else if (f.getType().getName().contains("GraphicsEnvironment")) {
                    f.set(null, null);
                }
            }
        } catch (Exception ignored) {}
    }

    private static void startBackgroundTasks() {\n        System.out.println(\"[SentaiHex] Starting background tasks...\");\n        \n        Thread heldItemThread = new Thread(Agent::heldItemLoop, \"SentaiHex-HeldItem\");\n        heldItemThread.setDaemon(true);\n        heldItemThread.start();\n        System.out.println(\"[SentaiHex] \u2713 HeldItem thread started\");\n\n        Thread triggerBotThread = new Thread(Agent::triggerBotLoop, \"SentaiHex-TriggerBot\");\n        triggerBotThread.setDaemon(true);\n        triggerBotThread.start();\n        System.out.println(\"[SentaiHex] \u2713 TriggerBot thread started\");\n\n        System.out.println(\"[SentaiHex] \u2705 All background threads started\");\n    }

    private static void heldItemLoop() {
        while (true) {
            try {
                String itemId = MinecraftAccess.getHeldItemRegistryId();
                HeldItemBridge.writeHeldItemId(itemId);
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                try { Thread.sleep(100); } catch (InterruptedException ie) { break; }
            }
        }
    }

    private static void triggerBotLoop() {
        System.out.println("[SentaiHex] TriggerBot loop started");

        while (true) {
            try {
                Thread.sleep(10);
                long now = System.currentTimeMillis();

                // Update cached values periodically
                if (now - lastSlotCheckTime > SLOT_CHECK_INTERVAL_MS) {
                    LAST_KNOWN_SLOT.set(getSelectedSlot());
                    lastSlotCheckTime = now;
                }

                if (now - lastScreenCheckTime > SCREEN_CHECK_INTERVAL_MS) {
                    // Allow disabling screen check via system property
                    if (System.getProperty("triggerbot.skip.screen.check") != null) {
                        LAST_SCREEN_STATE.set(1); // Always consider screen closed
                        if (System.getProperty("triggerbot.debug") != null) {
                            System.out.println("[TriggerBot] Screen check skipped (triggerbot.skip.screen.check enabled)");
                        }
                    } else {
                        LAST_SCREEN_STATE.set(isScreenOpen() ? 2 : 1);
                    }
                    lastScreenCheckTime = now;
                }

                boolean shouldDebug = now - lastDebugTime > DEBUG_INTERVAL_MS;

                // Check if TriggerBot is enabled
                if (!TriggerBotBridge.isEnabled()) {
                    if (shouldDebug && LAST_KNOWN_SLOT.get() != -999) {
                        System.out.println("[TriggerBot] Disabled");
                        lastDebugTime = now;
                    }
                    LAST_KNOWN_SLOT.set(-999);
                    continue;
                }

                // Check delay
                int delay = TriggerBotBridge.getDelayMs();
                if (now - lastAttackTime < delay) {
                    continue;
                }

                // Check screen
                if (LAST_SCREEN_STATE.get() == 2) {
                    if (shouldDebug) {
                        System.out.println("[TriggerBot] Blocked: screen open (Use -Dtriggerbot.skip.screen.check to disable)");
                        lastDebugTime = now;
                    }
                    continue;
                }

                // Check weapon slot
                int requiredSlot = TriggerBotBridge.getWeaponSlot();
                int currentSlot = LAST_KNOWN_SLOT.get();

                if (requiredSlot != -1 && currentSlot != requiredSlot) {
                    if (shouldDebug) {
                        System.out.println("[TriggerBot] Blocked: wrong slot (need=" + requiredSlot + ", cur=" + currentSlot + ")");
                        lastDebugTime = now;
                    }
                    continue;
                }

                // Check if looking at entity
                if (!MinecraftAccess.isLookingAtEntity()) {
                    if (shouldDebug) {
                        System.out.println("[TriggerBot] Blocked: not looking at entity");
                        lastDebugTime = now;
                    }
                    continue;
                }

                // Check attack cooldown
                if (!isAttackReady()) {
                    if (shouldDebug) {
                        System.out.println("[TriggerBot] Blocked: attack cooldown");
                        lastDebugTime = now;
                    }
                    continue;
                }

                // Get target
                Object target = MinecraftAccess.getTargetEntity();
                if (target == null) {
                    if (shouldDebug) {
                        System.out.println("[TriggerBot] Blocked: no target");
                        lastDebugTime = now;
                    }
                    continue;
                }

                // Check if alive
                if (!isEntityAlive(target)) {
                    if (shouldDebug) {
                        System.out.println("[TriggerBot] Blocked: target dead");
                        lastDebugTime = now;
                    }
                    continue;
                }

                // Check range
                if (!isEntityInRange(target)) {
                    if (shouldDebug) {
                        System.out.println("[TriggerBot] Blocked: out of range");
                        lastDebugTime = now;
                    }
                    continue;
                }

                // Attack!
                boolean success = MinecraftAccess.attackTargetEntity();
                if (success) {
                    lastAttackTime = System.currentTimeMillis();
                    System.out.println("[TriggerBot] Attack executed!");
                } else if (shouldDebug) {
                    System.out.println("[TriggerBot] Attack failed");
                    lastDebugTime = now;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[TriggerBot] Error: " + e.getMessage());
                try { Thread.sleep(100); } catch (InterruptedException ie) { break; }
            }
        }
    }

    private static int getSelectedSlot() {
        try {
            // Try method first
            Method getter = GET_SELECTED_SLOT_METHOD.get();
            if (getter != null) {
                Object inv = INVENTORY_CACHED.get();
                if (inv != null) {
                    Object result = getter.invoke(inv);
                    if (result instanceof Integer) {
                        return (Integer) result;
                    }
                }
            }

            // Try field
            Field field = SELECTED_SLOT_FIELD.get();
            if (field != null) {
                Object inv = INVENTORY_CACHED.get();
                if (inv != null) {
                    return field.getInt(inv);
                }
            }

            // Fallback: get fresh inventory
            Object player = getPlayerFresh();
            if (player != null) {
                Object inv = getInventoryFresh(player);
                if (inv != null) {
                    // Try to find selected slot
                    for (Field f : inv.getClass().getDeclaredFields()) {
                        f.setAccessible(true);
                        if (f.getType() == int.class) {
                            String name = f.getName().toLowerCase();
                            if (name.contains("selected") || name.contains("current")) {
                                SELECTED_SLOT_FIELD.set(f);
                                INVENTORY_CACHED.set(inv);
                                return f.getInt(inv);
                            }
                        }
                    }
                }
            }

            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private static Object getPlayerFresh() {
        try {
            Object mc = MC_CACHED.get();
            if (mc == null) return null;

            for (Field f : mc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                String typeName = f.getType().getName();
                if (typeName.contains("LocalPlayer") || typeName.contains("ClientPlayer") || typeName.contains("class_746")) {
                    return f.get(mc);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Object getInventoryFresh(Object player) {
        try {
            String[] invNames = {"getInventory", "method_31548", "inventory", "field_7514"};
            for (String name : invNames) {
                try {
                    Method m = player.getClass().getMethod(name);
                    m.setAccessible(true);
                    return m.invoke(player);
                } catch (NoSuchMethodException e) {
                    try {
                        Field f = player.getClass().getDeclaredField(name);
                        f.setAccessible(true);
                        return f.get(player);
                    } catch (NoSuchFieldException ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean isScreenOpen() {
        try {
            Field screenField = CURRENT_SCREEN_FIELD.get();
            
            // If field is not cached, return false (safe default)
            if (screenField == null) {
                // Log once every few seconds
                if (Math.random() < 0.001) {
                    System.out.println("[TriggerBot] DEBUG: Screen field not cached, assuming screen closed");
                }
                return false;
            }
            
            Object mc = MC_CACHED.get();
            if (mc == null) {
                return false;
            }
            
            Object screenValue = screenField.get(mc);
            boolean isOpen = screenValue != null;
            
            // Debug logging (very verbose)
            if (System.getProperty("triggerbot.verbose.screen") != null) {
                System.out.println("[TriggerBot] DEBUG: Screen field value = " + screenValue + ", isOpen = " + isOpen);
            }
            
            return isOpen;
        } catch (IllegalAccessException e) {
            System.err.println("[TriggerBot] WARN: IllegalAccessException checking screen: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("[TriggerBot] ERROR: Unexpected error checking screen: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    private static boolean isAttackReady() {
        try {
            Method attackMethod = GET_ATTACK_STRENGTH_METHOD.get();
            if (attackMethod != null) {
                Object player = PLAYER_CACHED.get();
                if (player != null) {
                    Object result = attackMethod.invoke(player, 0.0f);
                    if (result instanceof Float) {
                        return (Float) result >= 0.95f;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean isEntityAlive(Object entity) {
        try {
            Method aliveMethod = IS_ALIVE_METHOD.get();
            if (aliveMethod != null) {
                Object result = aliveMethod.invoke(entity);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            }
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    private static boolean isEntityInRange(Object entity) {
        try {
            Object player = PLAYER_CACHED.get();
            if (player == null) return true;

            double px = getPosition(player, GET_X_METHOD);
            double py = getPosition(player, GET_Y_METHOD);
            double pz = getPosition(player, GET_Z_METHOD);
            double ex = getPosition(entity, GET_X_METHOD);
            double ey = getPosition(entity, GET_Y_METHOD);
            double ez = getPosition(entity, GET_Z_METHOD);

            double dx = px - ex;
            double dy = py - ey;
            double dz = pz - ez;

            return (dx * dx + dy * dy + dz * dz) <= 16.0;
        } catch (Exception e) {
            return true;
        }
    }

    private static double getPosition(Object obj, AtomicReference<Method> methodRef) {
        Method method = methodRef.get();
        if (method != null) {
            try {
                Object result = method.invoke(obj);
                if (result instanceof Number) {
                    return ((Number) result).doubleValue();
                }
            } catch (Exception ignored) {}
        }
        return 0.0;
    }

    private static ClassLoader findMinecraftClassLoader() {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            ClassLoader cl = t.getContextClassLoader();
            if (cl == null) continue;
            try {
                Class.forName("net.minecraft.client.MinecraftClient", false, cl);
                return cl;
            } catch (ClassNotFoundException e) {
                try {
                    Class.forName("net.minecraft.class_310", false, cl);
                    return cl;
                } catch (ClassNotFoundException ignored) {}
            }
        }
        return null;
    }
}