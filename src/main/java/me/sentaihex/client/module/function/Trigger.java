package me.sentaihex.client.module.function;

import me.sentaihex.client.module.ClientModule;
import me.sentaihex.client.util.InputSimulator;
import me.sentaihex.client.util.MinecraftAccess;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

public class Trigger extends ClientModule {

    private int reactionDelay = 80;  // ms chờ trước khi click (giả reaction time)
    private int clickCooldown = 100; // ms chờ giữa các click

    private volatile boolean loopRunning = false;
    private Thread loopThread = null;

    public Trigger() {
        super("Trigger Bot", "Function", NativeKeyEvent.VC_H);
    }

    @Override
    public void onEnable() {
        System.out.println("[SentaiHex] TriggerBot ON");
        loopRunning = true;
        loopThread = new Thread(this::loop, "TriggerBot-Loop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    @Override
    public void onDisable() {
        System.out.println("[SentaiHex] TriggerBot OFF");
        loopRunning = false;
        if (loopThread != null) { loopThread.interrupt(); loopThread = null; }
    }

    @Override
    public void execute() { /* dùng loop riêng */ }

    private void loop() {
        // Retry init tối đa 10 lần (mỗi 500ms) — Render thread có thể chưa start kịp
        int retries = 0;
        while (!MinecraftAccess.init() && retries < 10) {
            try { Thread.sleep(500); } catch (InterruptedException ie) { return; }
            retries++;
        }
        if (!MinecraftAccess.init()) {
            System.err.println("[SentaiHex] TriggerBot: cannot access MC state after retries");
            toggle(); return;
        }
        System.out.println("[SentaiHex] TriggerBot: MC state OK, starting loop");
        while (loopRunning && !Thread.currentThread().isInterrupted()) {
            try {
                if (MinecraftAccess.isLookingAtEntity()) {
                    if (reactionDelay > 0) Thread.sleep(reactionDelay);
                    if (MinecraftAccess.isLookingAtEntity()) {
                        // Thử attack qua reflection trước (hoạt động SP + MP)
                        // Fallback về SendInput nếu reflection thất bại
                        boolean attacked = MinecraftAccess.attackTargetEntity();
                        if (!attacked) InputSimulator.leftClick();
                        Thread.sleep(clickCooldown);
                    }
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
        setGlobalDelay(ms);
        this.reactionDelay = Math.max(0, ms);
        this.clickCooldown = Math.max(50, ms + 20);
    }

    public int getDelay1() { return reactionDelay; }
    public void setDelay1(int d) { this.reactionDelay = Math.max(0, d); }
    public int getDelay2() { return clickCooldown; }
    public void setDelay2(int d) { this.clickCooldown = Math.max(50, d); }
}
