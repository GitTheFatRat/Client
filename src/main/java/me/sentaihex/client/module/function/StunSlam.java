package me.sentaihex.client.module.function;

import me.sentaihex.client.module.ClientModule;
import me.sentaihex.client.util.InputSimulator;
import me.sentaihex.client.util.MinecraftAccess;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

public class StunSlam extends ClientModule {

    private int slotAxe  = NativeKeyEvent.VC_Q;
    private int slotMace = NativeKeyEvent.VC_V;

    private int spamInterval = 55; // ms giữa các hit axe
    private int maceDelay    = 55; // ms trước khi đánh mace sau shield break

    private volatile boolean loopRunning = false;
    private Thread loopThread = null;

    public StunSlam() {
        super("Stun Slam", "Function", NativeKeyEvent.VC_J);
    }

    @Override
    public void onEnable() {
        System.out.println("[SentaiHex] StunSlam ON");
        loopRunning = true;
        loopThread = new Thread(this::loop, "StunSlam-Loop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    @Override
    public void onDisable() {
        System.out.println("[SentaiHex] StunSlam OFF");
        loopRunning = false;
        if (loopThread != null) { loopThread.interrupt(); loopThread = null; }
    }

    @Override
    public void execute() { /* dùng loop riêng */ }

    private void loop() {
        if (!MinecraftAccess.init()) {
            System.err.println("[SentaiHex] StunSlam: cannot access MC state");
            toggle(); return;
        }
        while (loopRunning && !Thread.currentThread().isInterrupted()) {
            try {
                Object target = MinecraftAccess.getTargetEntity();
                if (target == null) { Thread.sleep(10); continue; }

                if (MinecraftAccess.isEntityBlocking(target)) {
                    // Target đang hold shield → spam axe
                    InputSimulator.pressKeyThenLeftClick(
                            InputSimulator.nativeToWinVK(slotAxe));
                    Thread.sleep(spamInterval);
                } else if (MinecraftAccess.isLookingAtEntity()) {
                    // Shield vừa break → switch mace đánh ngay
                    InputSimulator.pressKeyThenLeftClick(
                            InputSimulator.nativeToWinVK(slotMace));
                    Thread.sleep(maceDelay);
                    // Switch lại axe sẵn sàng
                    InputSimulator.pressKeyThenLeftClick(
                            InputSimulator.nativeToWinVK(slotAxe));
                    Thread.sleep(spamInterval);
                } else {
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); break;
            }
        }
    }

    @Override
    public void setDelay(int ms) {
        int safe = Math.max(50, ms);
        setGlobalDelay(safe);
        this.spamInterval = safe;
        this.maceDelay    = safe;
    }

    public int getSlotAxe()            { return slotAxe; }
    public void setSlotAxe(int k)      { this.slotAxe = k; }
    public int getSlotMace()           { return slotMace; }
    public void setSlotMace(int k)     { this.slotMace = k; }
    public int getDelay1()             { return spamInterval; }
    public void setDelay1(int d)       { this.spamInterval = Math.max(50, d); }
    public int getDelay2()             { return maceDelay; }
    public void setDelay2(int d)       { this.maceDelay = Math.max(50, d); }
}