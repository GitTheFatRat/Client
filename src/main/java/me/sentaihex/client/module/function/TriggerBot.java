package me.sentaihex.client.module.function;

import me.sentaihex.client.module.ClientModule;
import me.sentaihex.client.util.MinecraftAccess;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadLocalRandom;

public class TriggerBot extends ClientModule {

    // --- Config ---
    private static final double RANGE_SQ           = 9.0;   // 3.0 blocks
    private static final int    JITTER_MS          = 15;
    private static final float  COOLDOWN_THRESHOLD = 0.9f;

    // --- Weapon slot filter (-1 = any) ---
    private int weaponSlot = -1;

    // --- Thread ---
    private volatile boolean running    = false;
    private Thread           tickThread = null;

    public TriggerBot() {
        super("Trigger Bot", "Function", -1);
    }

    public int  getWeaponSlot()         { return weaponSlot; }
    public void setWeaponSlot(int slot) { this.weaponSlot = slot; }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------
    @Override
    public void onEnable() {
        running    = true;
        tickThread = new Thread(this::tickLoop, "TriggerBot-Tick");
        tickThread.setDaemon(true);
        tickThread.start();
        System.out.println("[TriggerBot] enabled, delay=" + getGlobalDelay() + "ms");
    }

    @Override
    public void onDisable() {
        running = false;
        if (tickThread != null) {
            tickThread.interrupt();
            tickThread = null;
        }
        System.out.println("[TriggerBot] disabled");
    }

    @Override
    public void execute() throws InterruptedException {
        // Not used — TriggerBot is a Function, runs via onEnable thread
    }

    // -------------------------------------------------------------------------
    // Tick loop
    // -------------------------------------------------------------------------
    private void tickLoop() {
        // Try to init MinecraftAccess — retry every 2s if MC not ready yet
        System.out.println("[TriggerBot] thread started, waiting for Minecraft...");
        while (running) {
            if (MinecraftAccess.initMc()) break;
            try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
        }
        System.out.println("[TriggerBot] Minecraft found, starting tick loop");

        while (running && isEnabled()) {
            try {
                int base   = Math.max(50, getGlobalDelay());
                int jitter = ThreadLocalRandom.current().nextInt(-JITTER_MS, JITTER_MS + 1);
                Thread.sleep(Math.max(20, base + jitter));
                if (running && isEnabled()) tick();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("[TriggerBot] tick loop ended");
    }

    // -------------------------------------------------------------------------
    // One attack attempt
    // -------------------------------------------------------------------------
    private void tick() {
        // Step 1: screen open? skip
        if (isScreenOpen()) return;

        // Step 2: weapon slot filter
        if (weaponSlot != -1 && !isOnWeaponSlot()) return;

        // Step 3: attack cooldown (1.9+)
        if (!isCooldownReady()) return;

        // Step 4: crosshair on entity?
        // Try via MinecraftAccess.init() first, fallback to direct check
        boolean lookingAtEntity;
        if (MinecraftAccess.init()) {
            lookingAtEntity = MinecraftAccess.isLookingAtEntity();
        } else {
            // init() failed (hitResultField not found) — try direct entity check
            lookingAtEntity = isLookingAtEntityDirect();
        }
        if (!lookingAtEntity) return;

        // Step 5: get target
        Object target = getTarget();
        if (target == null) return;

        // Step 6: alive + in range
        if (!isAttackable(target)) return;
        if (!isInRange(target))    return;

        // Step 7: attack via MinecraftAccess or direct
        boolean hit;
        if (MinecraftAccess.init()) {
            hit = MinecraftAccess.attackTargetEntity();
        } else {
            hit = attackDirect(target);
        }

        if (hit) System.out.println("[TriggerBot] hit: " + target.getClass().getSimpleName());
    }

    // -------------------------------------------------------------------------
    // Direct crosshair check (bypasses hitResultField scan)
    // -------------------------------------------------------------------------
    private boolean isLookingAtEntityDirect() {
        try {
            Object mc = getMcInstance();
            if (mc == null) return false;
            // Scan all fields for hitResult-like object
            for (Field f : mc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(mc);
                if (val == null) continue;
                String cn = val.getClass().getName();
                if (cn.contains("EntityHitResult") || cn.contains("class_3965")
                        || (cn.contains("HitResult") && cn.toLowerCase().contains("entity"))) {
                    return true;
                }
                // Check type enum field inside HitResult
                if (cn.contains("HitResult") || cn.contains("class_239") || cn.contains("class_396")) {
                    for (Field ff : val.getClass().getSuperclass().getDeclaredFields()) {
                        ff.setAccessible(true);
                        if (ff.getType().isEnum()) {
                            String ev = ff.get(val).toString();
                            if (ev.contains("ENTITY")) return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    // -------------------------------------------------------------------------
    // Get target entity directly from hitResult
    // -------------------------------------------------------------------------
    private Object getTarget() {
        // Try MinecraftAccess first
        if (MinecraftAccess.init()) {
            return MinecraftAccess.getTargetEntity();
        }
        // Direct fallback
        try {
            Object mc = getMcInstance();
            if (mc == null) return null;
            for (Field f : mc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(mc);
                if (val == null) continue;
                String cn = val.getClass().getName();
                if (!cn.contains("HitResult") && !cn.contains("class_239")
                        && !cn.contains("class_396") && !cn.contains("class_3965")) continue;
                // Try to get entity from EntityHitResult
                for (String fname : new String[]{"entity", "field_5553"}) {
                    try {
                        Field ef = val.getClass().getDeclaredField(fname);
                        ef.setAccessible(true);
                        return ef.get(val);
                    } catch (NoSuchFieldException ignored) {}
                }
                // Fallback: first non-primitive field
                for (Field ef : val.getClass().getDeclaredFields()) {
                    ef.setAccessible(true);
                    if (!ef.getType().isPrimitive()) return ef.get(val);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // -------------------------------------------------------------------------
    // Direct attack (when MinecraftAccess.init() fails)
    // -------------------------------------------------------------------------
    private boolean attackDirect(Object target) {
        try {
            Object mc = getMcInstance();
            if (mc == null) return false;
            for (String mname : new String[]{"doAttack", "method_1596", "attack"}) {
                try {
                    // try with superclass chain to match param type
                    Class<?> entityClass = target.getClass();
                    while (entityClass != null) {
                        try {
                            Method m = mc.getClass().getDeclaredMethod(mname, entityClass);
                            m.setAccessible(true);
                            m.invoke(mc, target);
                            return true;
                        } catch (NoSuchMethodException ignored) {}
                        entityClass = entityClass.getSuperclass();
                    }
                } catch (Exception ignored) {}
            }
            // Fallback: scan for attack method with 1 entity param
            for (Method m : mc.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() != 1) continue;
                String pt = m.getParameterTypes()[0].getName();
                if (!pt.contains("Entity") && !pt.contains("class_1")) continue;
                String mn = m.getName().toLowerCase();
                if (!mn.contains("attack") && !mn.contains("hit")) continue;
                try {
                    m.setAccessible(true);
                    m.invoke(mc, target);
                    return true;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("[TriggerBot] attackDirect error: " + e.getMessage());
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Cooldown check
    // -------------------------------------------------------------------------
    private boolean isCooldownReady() {
        try {
            Object player = getPlayer();
            if (player == null) return true;
            for (String name : new String[]{"getAttackStrengthScale", "method_6039", "m_36349_"}) {
                try {
                    Method m = player.getClass().getMethod(name, float.class);
                    m.setAccessible(true);
                    Object r = m.invoke(player, 0.0f);
                    if (r instanceof Number n) return n.floatValue() >= COOLDOWN_THRESHOLD;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception ignored) {}
        return true;
    }

    // -------------------------------------------------------------------------
    // Attackable check
    // -------------------------------------------------------------------------
    private boolean isAttackable(Object entity) {
        try {
            for (String name : new String[]{"isAlive", "method_5805", "m_6083_"}) {
                try {
                    Method m = entity.getClass().getMethod(name);
                    m.setAccessible(true);
                    Object r = m.invoke(entity);
                    if (r instanceof Boolean b && !b) return false;
                    break;
                } catch (NoSuchMethodException ignored) {}
            }
            for (String name : new String[]{"isRemoved", "method_31559", "m_142529_"}) {
                try {
                    Method m = entity.getClass().getMethod(name);
                    m.setAccessible(true);
                    Object r = m.invoke(entity);
                    if (r instanceof Boolean b && b) return false;
                    break;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception ignored) {}
        return true;
    }

    // -------------------------------------------------------------------------
    // Range check
    // -------------------------------------------------------------------------
    private boolean isInRange(Object entity) {
        try {
            Object player = getPlayer();
            if (player == null) return true;
            double[] pp = getEntityPos(player);
            double[] tp = getEntityPos(entity);
            if (pp == null || tp == null) return true;
            double dx = pp[0] - tp[0], dy = pp[1] - tp[1], dz = pp[2] - tp[2];
            return (dx * dx + dy * dy + dz * dz) <= RANGE_SQ;
        } catch (Exception ignored) {}
        return true;
    }

    private double[] getEntityPos(Object entity) {
        try {
            double x = invokeDouble(entity, "getX", "method_23317", "m_20185_");
            double y = invokeDouble(entity, "getY", "method_23318", "m_20186_");
            double z = invokeDouble(entity, "getZ", "method_23321", "m_20189_");
            return new double[]{x, y, z};
        } catch (Exception ignored) {}
        return null;
    }

    private double invokeDouble(Object obj, String... names) {
        for (String name : names) {
            try {
                Method m = obj.getClass().getMethod(name);
                m.setAccessible(true);
                Object r = m.invoke(obj);
                if (r instanceof Number n) return n.doubleValue();
            } catch (NoSuchMethodException ignored) {}
            catch (Exception ignored) {}
        }
        return 0.0;
    }

    // -------------------------------------------------------------------------
    // Weapon slot filter
    // -------------------------------------------------------------------------
    private boolean isOnWeaponSlot() {
        try {
            Object player = getPlayer();
            if (player == null) return true;
            for (String invName : new String[]{"getInventory", "method_31548", "m_150109_"}) {
                try {
                    Method mi = player.getClass().getMethod(invName);
                    mi.setAccessible(true);
                    Object inv = mi.invoke(player);
                    if (inv == null) continue;
                    for (String slotName : new String[]{"selectedSlot", "selected", "field_7545"}) {
                        try {
                            Field f = inv.getClass().getDeclaredField(slotName);
                            f.setAccessible(true);
                            return (int) f.get(inv) == weaponSlot;
                        } catch (NoSuchFieldException ignored) {}
                    }
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception ignored) {}
        return true;
    }

    // -------------------------------------------------------------------------
    // Screen open check
    // -------------------------------------------------------------------------
    private boolean isScreenOpen() {
        try {
            Object mc = getMcInstance();
            if (mc == null) return false;
            for (String name : new String[]{"currentScreen", "screen", "field_1755", "f_91074_"}) {
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

    // -------------------------------------------------------------------------
    // Reflection base helpers
    // -------------------------------------------------------------------------
    private Object getMcInstance() {
        try {
            ClassLoader cl = null;
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if ("Render thread".equals(t.getName())) { cl = t.getContextClassLoader(); break; }
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
                        if (f.getType().equals(c) && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                            Object inst = f.get(null);
                            if (inst != null) return inst;
                        }
                    }
                } catch (ClassNotFoundException ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Object getPlayer() {
        try {
            Object mc = getMcInstance();
            if (mc == null) return null;
            for (String name : new String[]{"player", "thePlayer", "field_1724", "f_91074_"}) {
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