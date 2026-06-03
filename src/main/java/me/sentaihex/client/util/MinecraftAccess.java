package me.sentaihex.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MinecraftAccess {

    private static Object  mcInstance     = null;
    private static Field   hitResultField = null;
    private static boolean initialized   = false;

    public static synchronized boolean init() {
        if (initialized) return true;
        try {
            // Tìm Render thread — chứa classloader của Minecraft game
            ClassLoader mcCL = null;
            for (Thread t : Thread.getAllStackTraces().keySet()) {
                if ("Render thread".equals(t.getName())) {
                    mcCL = t.getContextClassLoader();
                    break;
                }
            }
            if (mcCL == null) {
                // Render thread chưa start, thử lại sau
                return false;
            }

            // Tìm MC class
            Class<?> mcClass = null;
            String[] candidates = {
                "net.minecraft.client.MinecraftClient", // Yarn remapped
                "net.minecraft.client.Minecraft",       // Mojmap
                "net.minecraft.class_310",              // Intermediary (Feather)
            };
            for (String name : candidates) {
                try {
                    mcClass = Class.forName(name, true, mcCL);
                    System.out.println("[SentaiHex] MC class: " + name);
                    break;
                } catch (ClassNotFoundException ignored) {}
            }
            if (mcClass == null) return false;

            // Singleton instance
            for (Field f : mcClass.getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getType().equals(mcClass) &&
                        java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    mcInstance = f.get(null);
                    if (mcInstance != null) break;
                }
            }
            if (mcInstance == null) return false;

            // field_1750 bị Feather reorder — type là Long thay vì HitResult
            // Cần scan theo type name chính xác
            // In tất cả field có type là class_ để debug
            // Scan để tìm HitResult field — class_3965/class_3966 là subclass trong Feather
            for (Field f : mcClass.getDeclaredFields()) {
                f.setAccessible(true);
                String t = f.getType().getName();
                if (!t.startsWith("net.minecraft")) continue;
                try {
                    Object val = f.get(mcInstance);
                    if (val == null) continue;
                    String vc = val.getClass().getName();
                    if (vc.equals("net.minecraft.class_3965")
                            || vc.equals("net.minecraft.class_3966")
                            || vc.contains("HitResult") || vc.contains("class_239")) {
                        hitResultField = f;
                        System.out.println("[SentaiHex] hitResult field: " + f.getName() + " (" + vc + ")");
                        break;
                    }
                } catch (Exception ignored) {}
                if (hitResultField == null && (t.contains("HitResult") || t.contains("class_239"))) {
                    hitResultField = f;
                    System.out.println("[SentaiHex] hitResult field by type: " + f.getName());
                    break;
                }
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
