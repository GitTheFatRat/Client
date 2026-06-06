package me.sentaihex.client.module.macros;

import me.sentaihex.client.module.ClientModule;
import me.sentaihex.client.util.InputSimulator;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

public class MaceTech1 extends ClientModule {

    private int slotPearl      = NativeKeyEvent.VC_4;
    private int slotWindCharge = NativeKeyEvent.VC_5;

    private int delay1 = 55;
    private int delay2 = 55;

    public MaceTech1() {
        super("Mace Tech 1 (Pearl+Wind)", "Macro", -1);
    }

    @Override
    public String getDisplayName() {
        return "Pearl + Wind";
    }

    @Override
    public java.util.Map<String, String> getSlots() {
        java.util.Map<String, String> slots = new java.util.LinkedHashMap<>();
        slots.put("slot1", "Pearl");
        slots.put("slot2", "Wind Charge");
        return slots;
    }

    @Override public void onEnable()  { System.out.println("[SentaiHex] MaceTech1 ON"); }
    @Override public void onDisable() { System.out.println("[SentaiHex] MaceTech1 OFF"); }

    @Override
    public void execute() throws InterruptedException {
        InputSimulator.pressKeyThenRightClick(InputSimulator.nativeToWinVK(slotPearl));
        Thread.sleep(delay1);

        InputSimulator.pressKeyThenRightClick(InputSimulator.nativeToWinVK(slotWindCharge));
        Thread.sleep(delay2);
    }

    @Override
    public void setDelay(int ms) {
        int safe = Math.max(50, ms);
        setGlobalDelay(safe);
        this.delay1 = safe;
        this.delay2 = safe;
    }

    public int getSlotPearl()            { return slotPearl; }
    public void setSlotPearl(int k)      { this.slotPearl = k; }
    public int getSlotWindCharge()       { return slotWindCharge; }
    public void setSlotWindCharge(int k) { this.slotWindCharge = k; }
    public int getDelay1() { return delay1; }
    public void setDelay1(int d) { this.delay1 = Math.max(50, d); }
    public int getDelay2() { return delay2; }
    public void setDelay2(int d) { this.delay2 = Math.max(50, d); }
}