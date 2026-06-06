package me.sentaihex.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.Set;

public class MinecraftAccess {

    private static Object         mcInstance    = null;
    private static ClassLoader    mcClassLoader = null;
    private static Field          hitResultField = null;
    private static boolean        mcInitialized  = false;
    private static boolean        initialized    = false;
    private static Instrumentation savedInst     = null;

    // Known intermediary field names for hitResult in class_310 (MinecraftClient)
    // Fabric 1.21.x: field_1761  (crosshairTarget / hitResult)
    private static final String[] HIT_RESULT_FIELD_NAMES = {
            "crosshairTarget", "hitResult", "field_1761",
            "currentScreen",   // never — just sentinel to stop
    };

    // -------------------------------------------------------------------------
    // Core init
    // -------------------------------------------------------------------------
    public static synchronized boolean initMc() { return initMc(null); }

    public static synchronized boolean initMc(Instrumentation inst) {
        if (inst != null) savedInst = inst;
        if (mcInitialized && mcInstance != null) return true;
        try {
            mcClassLoader = findMinecraftClassLoader();
            if (mcClassLoader == null && inst != null)
                mcClassLoader = findClassLoaderFromInstrumentation(inst);
            if (mcClassLoader == null) return false;

            Thread.currentThread().setContextClassLoader(mcClassLoader);

            Class<?> mcClass = resolveMcClass(mcClassLoader);
            if (mcClass == null) return false;

            // Open Minecraft module so reflection works on Java 21
            openMinecraftModule(mcClass, inst);

            mcInstance = resolveMcInstance(mcClass);
            if (mcInstance == null) return false;

            mcInitialized = true;
            System.out.println("[SentaiHex] MC class: " + mcClass.getName());
            return true;
        } catch (Exception e) {
            System.err.println("[SentaiHex] initMc error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Uses Instrumentation.redefineModule to open the Minecraft module to this
     * agent's unnamed module — required on Java 9+ / Java 21.
     */
    private static void openMinecraftModule(Class<?> mcClass, Instrumentation inst) {
        try {
            Module mcModule   = mcClass.getModule();
            Module thisModule = MinecraftAccess.class.getModule();
            if (!mcModule.isNamed()) return; // already open if unnamed
            if (mcModule.isOpen(mcClass.getPackageName(), thisModule)) return;

            if (inst != null) {
                // Open ALL packages in the Minecraft module to unnamed module
                Set<String> packages = mcModule.getPackages();
                java.util.Map<String, Set<Module>> extraOpens = new java.util.HashMap<>();
                Set<Module> allUnnamed = Collections.singleton(thisModule.getLayer() != null
                        ? thisModule : thisModule);
                for (String pkg : packages) {
                    extraOpens.put(pkg, Collections.singleton(thisModule));
                }
                inst.redefineModule(
                        mcModule,
                        Collections.emptySet(),   // extraReads
                        Collections.emptyMap(),   // extraExports
                        extraOpens,               // extraOpens
                        Collections.emptySet(),   // extraUses
                        Collections.emptyMap()    // extraProvides
                );
                System.out.println("[SentaiHex] Opened MC module: " + mcModule.getName());
            } else {
                // Fallback: use reflection to call Module.implAddOpens (internal API)
                try {
                    Method implAddOpens = Module.class.getDeclaredMethod(
                            "implAddOpens", String.class, Module.class);
                    implAddOpens.setAccessible(true);
                    for (String pkg : mcModule.getPackages()) {
                        implAddOpens.invoke(mcModule, pkg, thisModule);
                    }
                    System.out.println("[SentaiHex] Opened MC module via implAddOpens");
                } catch (Exception e2) {
                    System.err.println("[SentaiHex] Could not open MC module: " + e2.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[SentaiHex] openMinecraftModule error: " + e.getMessage());
        }
    }

    public static synchronized boolean init() {
        if (initialized) return true;
        if (!initMc()) return false;
        try {
            Class<?> mcClass = mcInstance.getClass();

            // Pass 1: scan by KNOWN field names (works even when value is null)
            for (String fname : HIT_RESULT_FIELD_NAMES) {
                if (fname.equals("currentScreen")) break; // sentinel
                try {
                    Field f = mcClass.getDeclaredField(fname);
                    f.setAccessible(true);
                    String typeName = f.getType().getName();
                    if (typeName.contains("HitResult") || typeName.contains("class_239")
                            || typeName.contains("class_396") || typeName.contains("class_3965")) {
                        hitResultField = f;
                        System.out.println("[SentaiHex] hitResult by name: " + fname + " type=" + typeName);
                        break;
                    }
                } catch (NoSuchFieldException ignored) {}
            }

            // Pass 2: scan all fields by TYPE name (catches obfuscated names)
            if (hitResultField == null) {
                for (Field f : mcClass.getDeclaredFields()) {
                    f.setAccessible(true);
                    String typeName = f.getType().getName();
                    if (typeName.contains("HitResult") || typeName.contains("class_239")
                            || typeName.contains("class_396")) {
                        hitResultField = f;
                        System.out.println("[SentaiHex] hitResult by type: " + f.getName() + " type=" + typeName);
                        break;
                    }
                }
            }

            // Pass 3: scan all fields by RUNTIME value (player in world, value non-null)
            if (hitResultField == null) {
                for (Field f : mcClass.getDeclaredFields()) {
                    f.setAccessible(true);
                    try {
                        Object val = f.get(mcInstance);
                        if (val == null) continue;
                        String cn = val.getClass().getName();
                        if (cn.contains("HitResult") || cn.contains("EntityHitResult")
                                || cn.contains("BlockHitResult") || cn.contains("class_3965")
                                || cn.contains("class_3966") || cn.contains("class_239")) {
                            hitResultField = f;
                            System.out.println("[SentaiHex] hitResult by value: " + f.getName() + " (" + cn + ")");
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }

            // Pass 4: scan superclass fields
            if (hitResultField == null) {
                Class<?> sup = mcClass.getSuperclass();
                while (sup != null && sup != Object.class) {
                    for (Field f : sup.getDeclaredFields()) {
                        f.setAccessible(true);
                        String typeName = f.getType().getName();
                        if (typeName.contains("HitResult") || typeName.contains("class_239")
                                || typeName.contains("class_396")) {
                            hitResultField = f;
                            System.out.println("[SentaiHex] hitResult in superclass " + sup.getName()
                                    + " field=" + f.getName());
                            break;
                        }
                    }
                    if (hitResultField != null) break;
                    sup = sup.getSuperclass();
                }
            }

            if (hitResultField == null) {
                System.err.println("[SentaiHex] hitResultField not found — dumping MC fields:");
                for (Field f : mcClass.getDeclaredFields()) {
                    System.err.println("  " + f.getName() + " : " + f.getType().getName());
                }
                // Don't block init — allow attackTargetEntity to still work via target scan
            }

            System.out.println("[SentaiHex] MinecraftAccess init OK (hitResult="
                    + (hitResultField != null ? hitResultField.getName() : "NOT FOUND") + ")");
            initialized = true;
            return true;

        } catch (Exception e) {
            System.err.println("[SentaiHex] init error: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Held item
    // -------------------------------------------------------------------------
    public static String getHeldItemRegistryId() {
        if (!initMc()) return "";
        try {
            Object player = getPlayer();
            if (player == null) return "empty";
            Object stack = getMainHandStack(player);
            if (stack == null || isStackEmpty(stack)) return "empty";
            Object item = invokeNoArg(stack, "getItem", "method_7909", "m_41720_");
            if (item == null) return "unknown";
            if (isExperienceBottleItem(item)) return "minecraft:experience_bottle";
            String descId = invokeString(stack, "getDescriptionId", "getTranslationKey",
                    "method_7922", "m_41467_");
            if (descId != null) {
                if (descId.contains("experience_bottle")) return "minecraft:experience_bottle";
                if (descId.startsWith("item.minecraft."))
                    return "minecraft:" + descId.substring("item.minecraft.".length());
            }
            String className = item.getClass().getName();
            if (className.contains("ExperienceBottle") || className.contains("class_1774"))
                return "minecraft:experience_bottle";
            String registryId = resolveRegistryId(item);
            if (registryId != null && !registryId.isBlank()) return registryId;
            return "unknown";
        } catch (Exception e) { return ""; }
    }

    public static boolean isHoldingExperienceBottle() {
        return "minecraft:experience_bottle".equals(getHeldItemRegistryId());
    }

    // -------------------------------------------------------------------------
    // Hit result / attack
    // -------------------------------------------------------------------------
    public static boolean isLookingAtEntity() {
        if (!init()) return false;
        if (hitResultField == null) return false;
        try {
            Object hr = hitResultField.get(mcInstance);
            if (hr == null) return false;
            String cn = hr.getClass().getName();
            // Direct EntityHitResult class check
            if (cn.contains("class_3965") || cn.toLowerCase().contains("entityhit")) return true;
            // class_239 is HitResult base — check its type enum field
            // In Fabric intermediary: field_1332 = type (HitResult.Type enum)
            // Type enum values: MISS, BLOCK, ENTITY
            for (String fname : new String[]{"field_1332", "type"}) {
                try {
                    Field f = hr.getClass().getDeclaredField(fname);
                    f.setAccessible(true);
                    Object val = f.get(hr);
                    if (val != null && val.toString().contains("ENTITY")) return true;
                } catch (NoSuchFieldException | IllegalAccessException ignored) {}
            }
            // Scan all fields for enum containing ENTITY
            Class<?> cls = hr.getClass();
            while (cls != null && cls != Object.class) {
                for (Field f : cls.getDeclaredFields()) {
                    try {
                        f.setAccessible(true);
                        if (!f.getType().isEnum()) continue;
                        Object val = f.get(hr);
                        if (val != null && val.toString().contains("ENTITY")) return true;
                    } catch (IllegalAccessException ignored) {}
                }
                cls = cls.getSuperclass();
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static Object getTargetEntity() {
        if (!init()) return null;
        if (hitResultField == null) return null;
        try {
            Object hr = hitResultField.get(mcInstance);
            if (hr == null) return null;
            if (!isLookingAtEntity()) return null;

            // In Fabric 1.21 intermediary, EntityHitResult extends HitResult (class_239)
            // The actual runtime object is class_3965 (EntityHitResult)
            // entity field: field_5553 in class_3965
            for (String fname : new String[]{"field_5553", "entity"}) {
                try {
                    // Search in actual runtime class and its hierarchy
                    Class<?> cls = hr.getClass();
                    while (cls != null && cls != Object.class) {
                        try {
                            Field f = cls.getDeclaredField(fname);
                            f.setAccessible(true);
                            Object val = f.get(hr);
                            if (val != null) return val;
                        } catch (NoSuchFieldException ignored) {}
                        cls = cls.getSuperclass();
                    }
                } catch (Exception ignored) {}
            }

            // Fallback: find first field whose type is an Entity subclass
            Class<?> cls = hr.getClass();
            while (cls != null && cls != Object.class) {
                for (Field f : cls.getDeclaredFields()) {
                    try {
                        f.setAccessible(true);
                        if (f.getType().isPrimitive()) continue;
                        String typeName = f.getType().getName();
                        // Skip Vec3, BlockPos, HitResult type enum
                        if (typeName.contains("Vec") || typeName.contains("Pos")
                                || typeName.contains("Block") || f.getType().isEnum()) continue;
                        Object val = f.get(hr);
                        if (val != null) return val;
                    } catch (IllegalAccessException ignored) {}
                }
                cls = cls.getSuperclass();
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static boolean attackTargetEntity() {
        if (!init()) return false;
        try {
            Object target = getTargetEntity();
            if (target == null) return false;

            // Walk superclass chain to find the right param type
            Class<?> paramClass = target.getClass();
            while (paramClass != null && paramClass != Object.class) {
                for (String mname : new String[]{"doAttack", "method_1596", "attack"}) {
                    try {
                        Method m = mcInstance.getClass().getDeclaredMethod(mname, paramClass);
                        m.setAccessible(true);
                        m.invoke(mcInstance, target);
                        return true;
                    } catch (NoSuchMethodException ignored) {}
                    // Also try on superclasses of mcInstance
                    Class<?> mcCls = mcInstance.getClass().getSuperclass();
                    while (mcCls != null && mcCls != Object.class) {
                        try {
                            Method m = mcCls.getDeclaredMethod(mname, paramClass);
                            m.setAccessible(true);
                            m.invoke(mcInstance, target);
                            return true;
                        } catch (NoSuchMethodException ignored2) {}
                        mcCls = mcCls.getSuperclass();
                    }
                }
                paramClass = paramClass.getSuperclass();
            }

            // Last resort: scan all methods on mcInstance
            Class<?> cls = mcInstance.getClass();
            while (cls != null && cls != Object.class) {
                for (Method m : cls.getDeclaredMethods()) {
                    if (m.getParameterCount() != 1) continue;
                    String pn = m.getParameterTypes()[0].getName();
                    if (!pn.contains("Entity") && !pn.contains("class_1")
                            && !pn.contains("class_16")) continue;
                    String mn = m.getName().toLowerCase();
                    if (!mn.contains("attack") && !mn.contains("hit")) continue;
                    m.setAccessible(true);
                    try {
                        m.invoke(mcInstance, target);
                        System.out.println("[SentaiHex] attack via scan: " + m.getName()
                                + "(" + m.getParameterTypes()[0].getSimpleName() + ")");
                        return true;
                    } catch (Exception ignored) {}
                }
                cls = cls.getSuperclass();
            }
        } catch (Exception e) {
            System.err.println("[SentaiHex] attackTargetEntity error: " + e.getMessage());
        }
        return false;
    }

    public static boolean isEntityBlocking(Object entity) {
        if (entity == null) return false;
        try {
            for (String mname : new String[]{"isBlocking", "method_6039"}) {
                try {
                    Method m = entity.getClass().getMethod(mname);
                    return (boolean) m.invoke(entity);
                } catch (NoSuchMethodException ignored) {}
            }
            for (Method m : entity.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("blocking")
                        && m.getParameterCount() == 0
                        && m.getReturnType() == boolean.class)
                    return (boolean) m.invoke(entity);
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static Object getRawHitResult() {
        if (!init()) return null;
        try { return hitResultField != null ? hitResultField.get(mcInstance) : null; }
        catch (Exception e) { return null; }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------
    private static ClassLoader findMinecraftClassLoader() {
        // 1. Try current context class loader
        ClassLoader contextCl = Thread.currentThread().getContextClassLoader();
        if (isMcClassLoader(contextCl)) return contextCl;

        // 2. Try all threads
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            ClassLoader cl = t.getContextClassLoader();
            if (isMcClassLoader(cl)) return cl;
        }

        // 3. Try Render thread specifically if not found
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if ("Render thread".equals(t.getName())) {
                ClassLoader cl = t.getContextClassLoader();
                if (cl != null) return cl;
            }
        }
        return null;
    }

    private static boolean isMcClassLoader(ClassLoader cl) {
        if (cl == null) return false;
        try {
            Class.forName("net.minecraft.class_310", false, cl);
            return true;
        } catch (Exception e) {
            try {
                Class.forName("net.minecraft.client.MinecraftClient", false, cl);
                return true;
            } catch (Exception e2) {
                try {
                    Class.forName("net.minecraft.client.Minecraft", false, cl);
                    return true;
                } catch (Exception e3) {
                    return false;
                }
            }
        }
    }

    private static ClassLoader findClassLoaderFromInstrumentation(Instrumentation inst) {
        String[] mcClasses = {
                "net.minecraft.client.MinecraftClient",
                "net.minecraft.client.Minecraft",
                "net.minecraft.class_310",
        };
        for (Class<?> cls : inst.getAllLoadedClasses()) {
            for (String target : mcClasses)
                if (cls.getName().equals(target) && cls.getClassLoader() != null)
                    return cls.getClassLoader();
        }
        for (Class<?> cls : inst.getAllLoadedClasses()) {
            if (cls.getName().startsWith("net.minecraft.client.") && cls.getClassLoader() != null)
                return cls.getClassLoader();
        }
        return null;
    }

    private static Class<?> resolveMcClass(ClassLoader cl) {
        for (String name : new String[]{
                "net.minecraft.client.MinecraftClient",
                "net.minecraft.client.Minecraft",
                "net.minecraft.class_310"}) {
            try { return Class.forName(name, true, cl); }
            catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    private static Object resolveMcInstance(Class<?> mcClass) {
        for (Field f : mcClass.getDeclaredFields()) {
            f.setAccessible(true);
            if (f.getType().equals(mcClass)
                    && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                try {
                    Object inst = f.get(null);
                    if (inst != null) return inst;
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public static Object getPlayer() {
        if (mcInstance == null) return null;
        // Try known field names
        for (String fname : new String[]{"player", "thePlayer", "field_1724"}) {
            try {
                Field f = mcInstance.getClass().getDeclaredField(fname);
                f.setAccessible(true);
                return f.get(mcInstance);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        }
        // Scan by type name
        for (Field f : mcInstance.getClass().getDeclaredFields()) {
            try {
                f.setAccessible(true);
                String typeName = f.getType().getName();
                if (typeName.contains("LocalPlayer") || typeName.contains("ClientPlayer")
                        || typeName.contains("class_746")) {
                    return f.get(mcInstance);
                }
            } catch (IllegalAccessException ignored) {}
        }
        return null;
    }

    private static Object getMainHandStack(Object player) {
        return invokeNoArg(player, "getMainHandStack", "getMainHandItem",
                "method_6047", "m_21205_");
    }

    private static boolean isStackEmpty(Object stack) {
        Boolean r = invokeBoolean(stack, "isEmpty", "method_7960", "m_41619_");
        return r == null || r;
    }

    private static boolean isExperienceBottleItem(Object item) {
        try {
            if (item.getClass().getName().contains("ExperienceBottle")) return true;
            for (String clsName : new String[]{
                    "net.minecraft.world.item.Items", "net.minecraft.class_1802"}) {
                try {
                    Class<?> items = Class.forName(clsName, true, mcClassLoader);
                    for (Field f : items.getDeclaredFields()) {
                        if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                        String fn = f.getName().toLowerCase();
                        if (!fn.contains("experience") && !fn.contains("field_8463")
                                && !fn.contains("field_8632")) continue;
                        f.setAccessible(true);
                        if (item.equals(f.get(null))) return true;
                    }
                } catch (ClassNotFoundException ignored) {}
            }
            return item.toString().toLowerCase().contains("experience_bottle");
        } catch (Exception ignored) {}
        return false;
    }

    private static String resolveRegistryId(Object item) {
        try {
            for (String regClassName : new String[]{
                    "net.minecraft.core.registries.BuiltInRegistries",
                    "net.minecraft.class_7923"}) {
                try {
                    Class<?> regClass = Class.forName(regClassName, true, mcClassLoader);
                    for (Field f : regClass.getDeclaredFields()) {
                        if (!f.getName().equals("ITEM") && !f.getName().equals("field_41178")) continue;
                        f.setAccessible(true);
                        Object registry = f.get(null);
                        if (registry == null) continue;
                        Object key = invokeRegistryKey(registry, item);
                        if (key == null) continue;
                        String path = invokeString(key, "getPath", "method_12832", "m_135815_");
                        String ns   = invokeString(key, "getNamespace", "method_12836", "m_135827_");
                        if (path != null && ns != null) return ns + ":" + path;
                        String raw = key.toString();
                        if (raw.contains(":")) return raw;
                    }
                } catch (ClassNotFoundException ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Object invokeRegistryKey(Object registry, Object item) {
        for (Method m : registry.getClass().getMethods()) {
            if (m.getParameterCount() != 1) continue;
            if (!m.getName().equals("getKey") && !m.getName().equals("method_10221")) continue;
            try { m.setAccessible(true); return m.invoke(registry, item); }
            catch (Exception ignored) {}
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String... names) {
        for (String name : names) {
            for (Method m : target.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().equals(name)) {
                    try { m.setAccessible(true); return m.invoke(target); }
                    catch (Exception ignored) {}
                }
            }
            try {
                Method m = target.getClass().getDeclaredMethod(name);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String invokeString(Object target, String... names) {
        for (String name : names) {
            Object val = invokeNoArg(target, name);
            if (val instanceof String s) return s;
        }
        return null;
    }

    private static Boolean invokeBoolean(Object target, String... names) {
        for (String name : names) {
            Object val = invokeNoArg(target, name);
            if (val instanceof Boolean b) return b;
        }
        return null;
    }
}