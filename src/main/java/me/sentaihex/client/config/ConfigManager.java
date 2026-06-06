package me.sentaihex.client.config;

import com.google.gson.*;
import me.sentaihex.client.SentaiHex;
import me.sentaihex.client.module.ClientModule;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {

    private static final String CONFIG_DIR  = System.getProperty("user.home") + "/.sentaihex/";
    private static final String CONFIG_FILE = CONFIG_DIR + "config.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Cache for reflection methods to improve performance
    private final Map<String, Method> getterCache = new ConcurrentHashMap<>();
    private final Map<String, Method> setterCache = new ConcurrentHashMap<>();

    private static final String[] SLOT_GETTERS = {
            "getSlotAnchor", "getSlotGlowstone", "getSlotTotem",
            "getSlotRail", "getSlotCart", "getSlotCrossbow",
            "getSlotPearl", "getSlotWindCharge", "getSlotFlint",
            "getSlotFireBow", "getSlotPrev", "getSlotSpear"
    };

    private static final String[] SLOT_SETTERS = {
            "setSlotAnchor", "setSlotGlowstone", "setSlotTotem",
            "setSlotRail", "setSlotCart", "setSlotCrossbow",
            "setSlotPearl", "setSlotWindCharge", "setSlotFlint",
            "setSlotFireBow", "setSlotPrev", "setSlotSpear"
    };

    private Method getCachedGetter(ClientModule m, String methodName) {
        String key = m.getClass().getName() + "#" + methodName;
        return getterCache.computeIfAbsent(key, k -> {
            try {
                Method method = m.getClass().getMethod(methodName);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                return null;
            }
        });
    }

    private Method getCachedSetter(ClientModule m, String methodName) {
        String key = m.getClass().getName() + "#" + methodName;
        return setterCache.computeIfAbsent(key, k -> {
            try {
                Method method = m.getClass().getMethod(methodName, int.class);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                return null;
            }
        });
    }

    public void save() {
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
            JsonObject root = new JsonObject();
            JsonArray modulesArr = new JsonArray();

            for (ClientModule m : SentaiHex.INSTANCE.moduleManager.getModules()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", m.getName());
                obj.addProperty("enabled", m.isEnabled());
                obj.addProperty("keybind", m.getKeybind());
                obj.addProperty("globalDelay", m.getGlobalDelay());

                // Save step delays if they differ from global
                int d1 = m.getDelay1();
                int d2 = m.getDelay2();
                int d3 = m.getDelay3();
                if (d1 != m.getGlobalDelay()) obj.addProperty("delay1", d1);
                if (d2 != m.getGlobalDelay()) obj.addProperty("delay2", d2);
                if (d3 != m.getGlobalDelay()) obj.addProperty("delay3", d3);

                // Save slot keys using cached reflection
                for (String getter : SLOT_GETTERS) {
                    String jsonKey = Character.toLowerCase(getter.charAt(3)) + getter.substring(4);
                    Method method = getCachedGetter(m, getter);
                    if (method != null) {
                        try {
                            int value = (int) method.invoke(m);
                            if (value != -1 && value != 0) {
                                obj.addProperty(jsonKey, value);
                            }
                        } catch (Exception ignored) {}
                    }
                }

                modulesArr.add(obj);
            }

            root.add("modules", modulesArr);
            Files.writeString(Paths.get(CONFIG_FILE), gson.toJson(root));
            System.out.println("[SentaiHex] Config saved successfully!");
        } catch (Exception e) {
            System.err.println("[SentaiHex] Failed to save config: " + e.getMessage());
        }
    }

    public void load() {
        try {
            if (!Files.exists(Paths.get(CONFIG_FILE))) {
                System.out.println("[SentaiHex] No existing config, using defaults.");
                return;
            }

            String json = Files.readString(Paths.get(CONFIG_FILE));
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray modulesArr = root.getAsJsonArray("modules");

            for (JsonElement el : modulesArr) {
                JsonObject obj = el.getAsJsonObject();
                String name = obj.get("name").getAsString();
                ClientModule m = SentaiHex.INSTANCE.moduleManager.getByName(name);
                if (m == null) continue;

                // Apply enabled state (use toggle to trigger onEnable/onDisable)
                boolean shouldEnable = obj.get("enabled").getAsBoolean();
                if (shouldEnable != m.isEnabled()) {
                    m.toggle();
                }

                m.setKeybind(obj.get("keybind").getAsInt());
                m.setGlobalDelay(obj.get("globalDelay").getAsInt());

                // Load step delays
                if (obj.has("delay1")) m.setDelay1(obj.get("delay1").getAsInt());
                if (obj.has("delay2")) m.setDelay2(obj.get("delay2").getAsInt());
                if (obj.has("delay3")) m.setDelay3(obj.get("delay3").getAsInt());

                // Load slot keys using cached reflection
                for (int i = 0; i < SLOT_SETTERS.length; i++) {
                    String setter = SLOT_SETTERS[i];
                    String getter = SLOT_GETTERS[i];
                    String jsonKey = Character.toLowerCase(getter.charAt(3)) + getter.substring(4);
                    if (obj.has(jsonKey)) {
                        Method method = getCachedSetter(m, setter);
                        if (method != null) {
                            try {
                                method.invoke(m, obj.get(jsonKey).getAsInt());
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }

            System.out.println("[SentaiHex] Config loaded successfully!");
        } catch (Exception e) {
            System.err.println("[SentaiHex] Failed to load config: " + e.getMessage());
        }
    }
}