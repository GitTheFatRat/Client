package me.sentaihex.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.instrument.Instrumentation;

public class MinecraftAccess {

    private static Object  mcInstance     = null;
    private static ClassLoader mcClassLoader = null;
    private static Field   hitResultField = null;
    private static boolean mcInitialized   = false;
    private static boolean initialized   = false;

    /** Core init — Minecraft instance only (no HitResult scan). Safe to call from the agent. */
    public static synchronized boolean initMc() {
        return initMc(null);
    }

    public static synchronized boolean initMc(Instrumentation inst) {
        if (mcInitialized && mcInstance != null) return true;
        try {
            mcClassLoader = findMinecraftClassLoader();
            if (mcClassLoader == null && inst != null) {
                mcClassLoader = findClassLoaderFromInstrumentation(inst);
            }
            if (mcClassLoader == null) return false;

            Thread.currentThread().setContextClassLoader(mcClassLoader);

            Class<?> mcClass = resolveMcClass(mcClassLoader);
            if (mcClass == null) return false;

            mcInstance = resolveMcInstance(mcClass);
            if (mcInstance == null) return false;

            mcInitialized = true;
            return true;
        } catch (Exception e) {
            System.err.println("[SentaiHex] MinecraftAccess initMc error: " + e.getMessage());
            return false;
        }
    }

    public static synchronized boolean init() {
        if (initialized) return true;
        if (!initMc()) return false;
        try {
            Class<?> mcClass = mcInstance.getClass();

            for (Field f : mcClass.getDeclaredFields()) {
                f.setAccessible(true);
                String typeName = f.getType().getName();
                if (typeName.contains("HitResult") || typeName.contains("class_239")
                        || typeName.contains("class_396")) {
                    hitResultField = f;
                    System.out.println("[SentaiHex] hitResult field by type: " + f.getName());
                    break;
                }
                if (!typeName.startsWith("net.minecraft")) continue;
                try {
                    Object val = f.get(mcInstance);
                    if (val == null) continue;
                    String valClass = val.getClass().getName();
                    if (valClass.contains("HitResult") || valClass.contains("EntityHitResult")
                            || valClass.equals("net.minecraft.class_3965")
                            || valClass.equals("net.minecraft.class_3966")
                            || valClass.contains("class_239")) {
                        hitResultField = f;
                        System.out.println("[SentaiHex] hitResult field: " + f.getName() + " (" + valClass + ")");
                        break;
                    }
                } catch (Exception ignored) {}
            }
            if (hitResultField == null) return false;

            System.out.println("[SentaiHex] MinecraftAccess init OK");
            initialized = true;
            return true;

        } catch (Exception e) {
            System.err.println("[SentaiHex] MinecraftAccess error: " + e.getMessage());
            return false;
        }
    }

    /** Returns registry id like {@code minecraft:experience_bottle}, or {@code empty} / {@code unknown}. */
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

            String descId = invokeString(stack, "getDescriptionId", "getTranslationKey", "method_7922", "m_41467_");
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
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isHoldingExperienceBottle() {
        String id = getHeldItemRegistryId();
        return "minecraft:experience_bottle".equals(id);
    }

    private static ClassLoader findMinecraftClassLoader() {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if ("Render thread".equals(t.getName())) {
                ClassLoader cl = t.getContextClassLoader();
                if (cl != null) return cl;
            }
        }
        return null;
    }

    private static ClassLoader findClassLoaderFromInstrumentation(Instrumentation inst) {
        String[] mcClasses = {
                "net.minecraft.client.MinecraftClient",
                "net.minecraft.client.Minecraft",
                "net.minecraft.class_310",
        };
        for (Class<?> cls : inst.getAllLoadedClasses()) {
            for (String target : mcClasses) {
                if (cls.getName().equals(target)) {
                    ClassLoader cl = cls.getClassLoader();
                    if (cl != null) return cl;
                }
            }
        }
        for (Class<?> cls : inst.getAllLoadedClasses()) {
            if (cls.getName().startsWith("net.minecraft.client.")) {
                ClassLoader cl = cls.getClassLoader();
                if (cl != null) return cl;
            }
        }
        return null;
    }

    private static boolean isExperienceBottleItem(Object item) {
        try {
            String cn = item.getClass().getName();
            if (cn.contains("ExperienceBottle")) return true;

            String[] itemsClasses = {
                    "net.minecraft.world.item.Items",
                    "net.minecraft.class_1802",
            };
            for (String clsName : itemsClasses) {
                try {
                    Class<?> items = Class.forName(clsName, true, mcClassLoader);
                    for (Field f : items.getDeclaredFields()) {
                        if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
                        String fn = f.getName().toLowerCase();
                        if (!fn.contains("experience") && !fn.contains("field_8463")
                                && !fn.contains("field_8632")) continue;
                        f.setAccessible(true);
                        Object ref = f.get(null);
                        if (item.equals(ref)) return true;
                    }
                } catch (ClassNotFoundException ignored) {}
            }

            String raw = item.toString().toLowerCase();
            return raw.contains("experience_bottle");
        } catch (Exception ignored) {}
        return false;
    }

    private static Class<?> resolveMcClass(ClassLoader cl) {
        String[] candidates = {
                "net.minecraft.client.MinecraftClient",
                "net.minecraft.client.Minecraft",
                "net.minecraft.class_310",
        };
        for (String name : candidates) {
            try {
                Class<?> mcClass = Class.forName(name, true, cl);
                System.out.println("[SentaiHex] MC class: " + name);
                return mcClass;
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    private static Object resolveMcInstance(Class<?> mcClass) throws IllegalAccessException {
        for (Field f : mcClass.getDeclaredFields()) {
            f.setAccessible(true);
            if (f.getType().equals(mcClass)
                    && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                Object inst = f.get(null);
                if (inst != null) return inst;
            }
        }
        return null;
    }

    private static Object getMainHandStack(Object player) {
        Object stack = invokeNoArg(player, "getMainHandItem", "method_6047", "m_21205_");
        if (stack != null) return stack;
        for (Method m : player.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            String rt = m.getReturnType().getName();
            if (!rt.contains("ItemStack") && !rt.contains("class_1799")) continue;
            try {
                m.setAccessible(true);
                return m.invoke(player);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Object getPlayer() throws IllegalAccessException {
        for (String name : new String[]{"player", "thePlayer", "field_1724", "f_91074_"}) {
            try {
                Field f = mcInstance.getClass().getDeclaredField(name);
                f.setAccessible(true);
                return f.get(mcInstance);
            } catch (NoSuchFieldException ignored) {}
        }
        for (Field f : mcInstance.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            String t = f.getType().getName();
            if (t.contains("LocalPlayer") || t.contains("ClientPlayer") || t.contains("class_746"))
                return f.get(mcInstance);
        }
        return null;
    }

    private static boolean isStackEmpty(Object stack) {
        Boolean result = invokeBoolean(stack, "isEmpty", "method_7960", "m_41619_");
        return result == null || result;
    }

    private static Object invokeNoArg(Object target, String... names) {
        for (String name : names) {
            for (Method m : target.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().equals(name)) {
                    try {
                        m.setAccessible(true);
                        return m.invoke(target);
                    } catch (Exception ignored) {}
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

    private static String resolveRegistryId(Object item) {
        try {
            String[] registryClasses = {
                    "net.minecraft.core.registries.BuiltInRegistries",
                    "net.minecraft.class_7923",
            };
            for (String regClassName : registryClasses) {
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
            try {
                m.setAccessible(true);
                return m.invoke(registry, item);
            } catch (Exception ignored) {}
        }
        return null;
    }

    /** Trả về raw hitResult object để debug */
    public static Object getRawHitResult() {
        if (!init()) return null;
        try { return hitResultField.get(mcInstance); }
        catch (Exception e) { return null; }
    }


    public static boolean isLookingAtEntity() {
        if (!init()) return false;
        try {
            Object hr = hitResultField.get(mcInstance);
            if (hr == null) return false;
            String cn = hr.getClass().getName();
            // class_3965 = EntityHitResult (Feather intermediary)
            if (cn.equals("net.minecraft.class_3965")) return true;
            if (cn.toLowerCase().contains("entity")) return true;
            for (Field f : hr.getClass().getSuperclass().getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType().isEnum()) {
                    String val = f.get(hr).toString();
                    return val.contains("ENTITY");
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static Object getTargetEntity() {
        if (!init()) return null;
        try {
            Object hr = hitResultField.get(mcInstance);
            if (hr == null) return null;
            String cn = hr.getClass().getName();
            if (!cn.equals("net.minecraft.class_3965") && !cn.toLowerCase().contains("entity"))
                return null;
            // Thử tên field entity/field_5553
            for (String fname : new String[]{"entity", "field_5553"}) {
                try {
                    Field f = hr.getClass().getDeclaredField(fname);
                    f.setAccessible(true);
                    return f.get(hr);
                } catch (NoSuchFieldException ignored) {}
            }
            // Fallback: field đầu tiên không phải primitive
            for (Field f : hr.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                if (!f.getType().isPrimitive()) return f.get(hr);
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Gọi attack action trực tiếp trong Minecraft — hoạt động cả SP lẫn MP.
     * Tương đương với việc player nhấn chuột trái đánh entity.
     * Dùng reflection để gọi method doAttack(entity) trong GameRenderer/MinecraftClient.
     */
    public static boolean attackTargetEntity() {
        if (!init()) return false;
        try {
            Object target = getTargetEntity();
            if (target == null) return false;

            // Tìm method attack trong MinecraftClient
            // Yarn: method_1596 hoặc tên remapped "doAttack"
            // Mojmap: attack
            for (String mname : new String[]{"doAttack", "method_1596", "attack"}) {
                try {
                    java.lang.reflect.Method m = mcInstance.getClass()
                        .getDeclaredMethod(mname, target.getClass().getSuperclass().getSuperclass());
                    m.setAccessible(true);
                    m.invoke(mcInstance, target);
                    return true;
                } catch (NoSuchMethodException ignored) {}
            }

            // Fallback: tìm method có 1 param kiểu Entity
            for (java.lang.reflect.Method m : mcInstance.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() != 1) continue;
                String paramType = m.getParameterTypes()[0].getName();
                if (!paramType.contains("Entity") && !paramType.contains("class_1")) continue;
                String mname = m.getName().toLowerCase();
                if (!mname.contains("attack") && !mname.contains("hit")) continue;
                m.setAccessible(true);
                try {
                    m.invoke(mcInstance, target);
                    return true;
                } catch (Exception ignored) {}
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
}
