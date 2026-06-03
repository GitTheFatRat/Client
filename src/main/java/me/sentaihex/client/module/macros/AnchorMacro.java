package me.sentaihex.client.module.macros;

import me.sentaihex.client.module.ClientModule;
import me.sentaihex.client.util.InputSimulator;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

public class AnchorMacro extends ClientModule {

    private int slotAnchor    = NativeKeyEvent.VC_X;
    private int slotGlowstone = NativeKeyEvent.VC_C;
    private int slotTotem     = NativeKeyEvent.VC_TAB;

    // key+click gửi đồng thời trong 1 batch → server nhận đúng thứ tự
    // delay chỉ cần đủ để server tick xử lý xong bước trước (1 tick = 50ms)
    private int delay1 = 55;
    private int delay2 = 55;
    private int delay3 = 55;

    public AnchorMacro() {
        super("Anchor Bomb", "Macro", NativeKeyEvent.VC_G);
    }

    @Override
    public void onEnable()  { System.out.println("[SentaiHex] AnchorMacro ON - Keybind: " + getKeybindName()); }
    @Override
    public void onDisable() { System.out.println("[SentaiHex] AnchorMacro OFF"); }

    @Override
    public void execute() throws InterruptedException {
        // Gửi keydown+keyup+rightdown+rightup trong 1 SendInput call → không bao giờ lệch nhau
        InputSimulator.pressKeyThenRightClick(InputSimulator.nativeToWinVK(slotAnchor));
        Thread.sleep(delay1);

        InputSimulator.pressKeyThenRightClick(InputSimulator.nativeToWinVK(slotGlowstone));
        Thread.sleep(delay2);

        InputSimulator.pressKeyThenRightClick(InputSimulator.nativeToWinVK(slotTotem));
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

    public int getSlotAnchor()          { return slotAnchor; }
    public void setSlotAnchor(int k)    { this.slotAnchor = k; }
    public int getSlotGlowstone()       { return slotGlowstone; }
    public void setSlotGlowstone(int k) { this.slotGlowstone = k; }
    public int getSlotTotem()           { return slotTotem; }
    public void setSlotTotem(int k)     { this.slotTotem = k; }

    public int getDelay1() { return delay1; }
    public void setDelay1(int d) { this.delay1 = Math.max(50, d); }
    public int getDelay2() { return delay2; }
    public void setDelay2(int d) { this.delay2 = Math.max(50, d); }
    public int getDelay3() { return delay3; }
    public void setDelay3(int d) { this.delay3 = Math.max(50, d); }
}