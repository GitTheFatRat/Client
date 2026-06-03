package me.sentaihex.client.config;

import com.google.gson.*;
import me.sentaihex.client.SentaiHex;
import me.sentaihex.client.module.ClientModule;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.*;

public class ConfigManager {

    private static final String CONFIG_DIR  = System.getProperty("user.home") + "/.sentaihex/";
    private static final String CONFIG_FILE = CONFIG_DIR + "config.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // [FIX #4] Danh sách tên getter/setter của các slot key cần lưu — thêm vào đây nếu có macro mới
    private static final String[] SLOT_GETTERS = {
        "getSlotAnchor", "getSlotGlowstone", "getSlotTotem",  // AnchorMacro
        "getSlotRail",   "getSlotCart",      "getSlotCrossbow", // TNTCartMacro
        "getSlotPearl",  "getSlotWindCharge",                   // MaceTech1
        "getSlotAxe",    "getSlotMace"                          // StunSlam
    };
    private static final String[] SLOT_SETTERS = {
        "setSlotAnchor", "setSlotGlowstone", "setSlotTotem",
        "setSlotRail",   "setSlotCart",      "setSlotCrossbow",
        "setSlotPearl",  "setSlotWindCharge",
        "setSlotAxe",    "setSlotMace"
    };

    public void save() {
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
            JsonObject root = new JsonObject();
            JsonArray modulesArr = new JsonArray();

            for (ClientModule m : SentaiHex.INSTANCE.moduleManager.getModules()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name",        m.getName());
                obj.addProperty("enabled",     m.isEnabled());
                obj.addProperty("keybind",     m.getKeybind());
                obj.addProperty("globalDelay", m.getGlobalDelay());

                // Lưu delay1 / delay2 / delay3
                invokeGetterInt(m, "getDelay1", obj, "delay1");
                invokeGetterInt(m, "getDelay2", obj, "delay2");
                invokeGetterInt(m, "getDelay3", obj, "delay3");

                // [FIX #4] Lưu toàn bộ slot keys
                for (String getter : SLOT_GETTERS) {
                    // Tên JSON key = bỏ "get" ở đầu, chữ thường chữ cái đầu
                    String jsonKey = Character.toLowerCase(getter.charAt(3)) + getter.substring(4);
                    invokeGetterInt(m, getter, obj, jsonKey);
                }

                modulesArr.add(obj);
            }

            root.add("modules", modulesArr);
            Files.writeString(Paths.get(CONFIG_FILE), gson.toJson(root));
            System.out.println("[SentaiHex] Config đã được lưu thành công!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        try {
            if (!Files.exists(Paths.get(CONFIG_FILE))) {
                System.out.println("[SentaiHex] Không tìm thấy config cũ, dùng mặc định.");
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

                m.setEnabled(obj.get("enabled").getAsBoolean());
                m.setKeybind(obj.get("keybind").getAsInt());
                m.setGlobalDelay(obj.get("globalDelay").getAsInt());

                // Load delay1 / delay2 / delay3
                invokeSetterInt(m, "setDelay1", obj, "delay1");
                invokeSetterInt(m, "setDelay2", obj, "delay2");
                invokeSetterInt(m, "setDelay3", obj, "delay3");

                // [FIX #4] Load toàn bộ slot keys
                for (int i = 0; i < SLOT_SETTERS.length; i++) {
                    String setter  = SLOT_SETTERS[i];
                    String getter  = SLOT_GETTERS[i];
                    String jsonKey = Character.toLowerCase(getter.charAt(3)) + getter.substring(4);
                    invokeSetterInt(m, setter, obj, jsonKey);
                }
            }

            System.out.println("[SentaiHex] Config được tải lên thành công!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Helper: gọi getter trả int rồi ghi vào JsonObject ---
    private void invokeGetterInt(ClientModule m, String methodName, JsonObject obj, String jsonKey) {
        try {
            Method method = m.getClass().getMethod(methodName);
            obj.addProperty(jsonKey, (Integer) method.invoke(m));
        } catch (Exception ignored) {}
    }

    // --- Helper: đọc int từ JsonObject rồi gọi setter ---
    private void invokeSetterInt(ClientModule m, String methodName, JsonObject obj, String jsonKey) {
        if (!obj.has(jsonKey)) return;
        try {
            Method method = m.getClass().getMethod(methodName, int.class);
            method.invoke(m, obj.get(jsonKey).getAsInt());
        } catch (Exception ignored) {}
    }
}
