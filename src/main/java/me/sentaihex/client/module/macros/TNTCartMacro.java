package me.sentaihex.client.module.macros;

import me.sentaihex.client.module.ClientModule;
import me.sentaihex.client.util.InputSimulator;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

public class TNTCartMacro extends ClientModule {

    private int slotRail     = NativeKeyEvent.VC_F12;
    private int slotCart     = NativeKeyEvent.VC_F9;
    private int slotCrossbow = NativeKeyEvent.VC_8;

    private int delay1 = 55;
    private int delay2 = 55;
    private int delay3 = 55;

    public TNTCartMacro() {
        super("TNT Cart", "Macro", -1);
    }

    @Override public void onEnable()  { System.out.println("[SentaiHex] TNTCartMacro ON"); }
    @Override public void onDisable() { System.out.println("[SentaiHex] TNTCartMacro OFF"); }

    @Override
    public void execute() throws InterruptedException {
        InputSimulator.pressKeyThenRightClick(InputSimulator.nativeToWinVK(slotRail));
        Thread.sleep(delay1);

        InputSimulator.pressKeyThenRightClick(InputSimulator.nativeToWinVK(slotCart));
        Thread.sleep(delay2);

        InputSimulator.pressKeyThenRightClick(InputSimulator.nativeToWinVK(slotCrossbow));
        Thread.sleep(delay3);
    }

    @Override
    public void setDelay(int ms) {
        int safe = Math.max(50, ms);
        setGlobalDelay(safe);
        this.delay1 = safe;
        this.delay2 = safe;
        this.delay3 = safe;
    }

    public int getSlotRail()           { return slotRail; }
    public void setSlotRail(int k)     { this.slotRail = k; }
    public int getSlotCart()           { return slotCart; }
    public void setSlotCart(int k)     { this.slotCart = k; }
    public int getSlotCrossbow()       { return slotCrossbow; }
    public void setSlotCrossbow(int k) { this.slotCrossbow = k; }
    public int getDelay1() { return delay1; }
    public void setDelay1(int d) { this.delay1 = Math.max(50, d); }
    public int getDelay2() { return delay2; }
    public void setDelay2(int d) { this.delay2 = Math.max(50, d); }
    public int getDelay3() { return delay3; }
    public void setDelay3(int d) { this.delay3 = Math.max(50, d); }
}