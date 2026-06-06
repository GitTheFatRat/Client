package me.sentaihex.client.module.function;

import me.sentaihex.client.module.ClientModule;
import me.sentaihex.client.util.TriggerBotBridge;

/**
 * Launcher-side TriggerBot module.
 * Actual attack logic runs inside the Minecraft JVM via Agent + TriggerBotBridge.
 * This class only manages enabled state and config — no reflection needed here.
 */
public class TriggerBot extends ClientModule {

    private int weaponSlot = -1;

    public TriggerBot() {
        super("Trigger Bot", "Function", -1);
    }

    public int  getWeaponSlot()         { return weaponSlot; }
    public void setWeaponSlot(int slot) {
        this.weaponSlot = slot;
        TriggerBotBridge.writeConfig(isEnabled(), weaponSlot, getGlobalDelay());
    }

    @Override
    public void onEnable() {
        TriggerBotBridge.writeConfig(true, weaponSlot, getGlobalDelay());
        System.out.println("[TriggerBot] enabled — agent will handle attacks");
    }

    @Override
    public void onDisable() {
        TriggerBotBridge.writeConfig(false, weaponSlot, getGlobalDelay());
        System.out.println("[TriggerBot] disabled");
    }

    @Override
    public void execute() throws InterruptedException {
        // Not used — TriggerBot is a Function toggled via keybind
    }

    @Override
    public void setDelay(int ms) {
        super.setDelay(ms);
        // Push updated delay to agent immediately
        TriggerBotBridge.writeConfig(isEnabled(), weaponSlot, getGlobalDelay());
    }
}