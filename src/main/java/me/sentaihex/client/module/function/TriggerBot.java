package me.sentaihex.client.module.function;

import me.sentaihex.client.module.ClientModule;
import me.sentaihex.client.util.TriggerBotBridge;

public class TriggerBot extends ClientModule {

    private int weaponSlot = 0; // Mặc định slot 0 = kiếm

    public TriggerBot() {
        super("Trigger Bot", "Function", -1);
    }

    @Override
    public boolean hasFunctionBody() {
        return true;
    }

    public int getWeaponSlot() {
        return weaponSlot;
    }

    public void setWeaponSlot(int slot) {
        this.weaponSlot = slot;
        if (isEnabled()) {
            TriggerBotBridge.writeConfig(true, weaponSlot, getGlobalDelay());
        } else {
            TriggerBotBridge.writeConfig(false, weaponSlot, getGlobalDelay());
        }
        System.out.println("[TriggerBot] Weapon slot set to: " + slot + " (0=slot1/sword)");
    }

    @Override
    public void onEnable() {
        TriggerBotBridge.writeConfig(true, weaponSlot, getGlobalDelay());
        System.out.println("[TriggerBot] Enabled - weapon slot: " + weaponSlot + " (0=slot1/sword)");
    }

    @Override
    public void onDisable() {
        TriggerBotBridge.writeConfig(false, weaponSlot, getGlobalDelay());
        System.out.println("[TriggerBot] Disabled");
    }

    @Override
    public void execute() throws InterruptedException {
        // Not used - TriggerBot is toggled via keybind
    }

    @Override
    public void setDelay(int ms) {
        super.setDelay(ms);
        if (isEnabled()) {
            TriggerBotBridge.writeConfig(true, weaponSlot, getGlobalDelay());
        }
    }
}