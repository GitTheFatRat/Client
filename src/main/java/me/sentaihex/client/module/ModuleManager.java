package me.sentaihex.client.module;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import me.sentaihex.client.module.macros.AnchorMacro;
import me.sentaihex.client.module.macros.MaceTech1;
import me.sentaihex.client.module.macros.TNTCartMacro;

import java.util.ArrayList;
import java.util.List;

public class ModuleManager implements NativeKeyListener {

    private final List<ClientModule> modules = new ArrayList<>();

    public ModuleManager() {
        register(new AnchorMacro());
        register(new TNTCartMacro());
        register(new MaceTech1());

        GlobalScreen.addNativeKeyListener(this);
        System.out.println("[SentaiHex] ModuleManager loaded " + modules.size() + " modules");
    }

    private void register(ClientModule module) { modules.add(module); }

    // Lưu timestamp lần cuối toggle cho từng module — tránh key-repeat trigger nhiều lần
    private final java.util.Map<ClientModule, Long> lastToggleTime = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long TOGGLE_DEBOUNCE_MS = 250;

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        int code = e.getKeyCode();
        long now = System.currentTimeMillis();
        for (ClientModule m : modules) {
            if (m.getKeybind() != code) continue;
            if ("Function".equals(m.getCategory())) {
                // Debounce: bỏ qua nếu vừa toggle trong 250ms
                long last = lastToggleTime.getOrDefault(m, 0L);
                if (now - last < TOGGLE_DEBOUNCE_MS) continue;
                lastToggleTime.put(m, now);
                m.toggle();
            } else {
                m.triggerIfEnabled(code);
            }
        }
    }

    @Override public void nativeKeyReleased(NativeKeyEvent e) {}
    @Override public void nativeKeyTyped(NativeKeyEvent e) {}

    public List<ClientModule> getModules() { return modules; }

    public List<ClientModule> getByCategory(String category) {
        List<ClientModule> result = new ArrayList<>();
        for (ClientModule m : modules)
            if (m.getCategory().equals(category)) result.add(m);
        return result;
    }

    public ClientModule getByName(String name) {
        for (ClientModule m : modules)
            if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }
}
