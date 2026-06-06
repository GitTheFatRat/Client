package me.sentaihex.client.module;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ClientModule {

    private String name;
    private String category;
    private boolean enabled = false;
    private int keybind = -1;

    private int globalDelay = 100;
    private int[] stepDelays;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public ClientModule(String name, String category, int defaultKey) {
        this.name = name;
        this.category = category;
        this.keybind = defaultKey;
    }

    public String getDisplayName() {
        return name;
    }

    public java.util.Map<String, String> getSlots() {
        return java.util.Collections.emptyMap();
    }

    public boolean hasFunctionBody() {
        return false;
    }

    public abstract void onEnable();
    public abstract void onDisable();
    public abstract void execute() throws InterruptedException;

    public void toggle() {
        if (!enabled) {
            onEnable();
            setEnabled(true);
        } else {
            onDisable();
            setEnabled(false);
        }
    }

    public void triggerIfEnabled(int keyCode) {
        if (!enabled) return;
        if (keybind == -1 || keyCode != keybind) return;

        if (!running.compareAndSet(false, true)) return;

        Thread t = new Thread(() -> {
            try {
                execute();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[SentaiHex] Error in " + name + ": " + e.getMessage());
            } finally {
                running.set(false);
            }
        }, name + "-Thread");
        t.setDaemon(true);
        t.start();
    }

    public boolean isRunning() {
        return running.get();
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean newValue) {
        boolean old = this.enabled;
        this.enabled = newValue;
        pcs.firePropertyChange("enabled", old, newValue);
    }

    public int getKeybind() { return keybind; }
    public void setKeybind(int keybind) { this.keybind = keybind; }

    // ====================== DELAY FUNCTIONS ======================

    public int getGlobalDelay() {
        return globalDelay;
    }

    public void setGlobalDelay(int delay) {
        this.globalDelay = Math.max(1, delay);
    }

    public void setDelay(int ms) {
        setGlobalDelay(ms);
        System.out.println("[SentaiHex] " + name + " delay set to " + ms + "ms");
    }

    public int[] getStepDelays() {
        return stepDelays;
    }

    public void setStepDelays(int[] s) {
        this.stepDelays = s;
    }

    public int getStepDelay(int step) {
        if (stepDelays != null && step < stepDelays.length)
            return stepDelays[step];
        return globalDelay;
    }

    // ====================== MULTI-DELAY GETTERS/SETTERS ======================
    // These are overridden by macros that have multiple delay values
    // Default implementation returns global delay for all

    public int getDelay1() {
        return globalDelay;
    }

    public int getDelay2() {
        return globalDelay;
    }

    public int getDelay3() {
        return globalDelay;
    }

    public void setDelay1(int ms) {
        setGlobalDelay(ms);
    }

    public void setDelay2(int ms) {
        setGlobalDelay(ms);
    }

    public void setDelay3(int ms) {
        setGlobalDelay(ms);
    }

    public String getKeybindName() {
        if (keybind == -1) return "NONE";
        return NativeKeyEvent.getKeyText(keybind);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }
}