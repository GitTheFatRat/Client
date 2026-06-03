package me.sentaihex.client.module.function;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import me.sentaihex.client.module.ClientModule;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class XPBottleSpammer extends ClientModule implements NativeMouseListener {

    private static final int THROW_INTERVAL_MS = 50;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "XPBottleSpammer-Thread");
                t.setDaemon(true);
                return t;
            });

    private ScheduledFuture<?> task = null;
    private Robot robot;

    public XPBottleSpammer() {
        super("XP Bottle Spam", "Function", -1);
        try {
            robot = new Robot();
        } catch (AWTException e) {
            System.err.println("[SentaiHex] XPBottleSpammer: Robot init failed - " + e.getMessage());
        }
    }

    @Override
    public void onEnable() {
        GlobalScreen.addNativeMouseListener(this);
    }

    @Override
    public void onDisable() {
        GlobalScreen.removeNativeMouseListener(this);
        stopSpam();
    }

    @Override
    public void execute() {
        // Không dùng triggerIfEnabled - module dùng mouse hook riêng
    }

    // ─── MOUSE HOOK ───────────────────────────────────────────────────────────

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        if (e.getButton() == NativeMouseEvent.BUTTON2) {
            startSpam();
        }
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        if (e.getButton() == NativeMouseEvent.BUTTON2) {
            stopSpam();
        }
    }

    @Override public void nativeMouseClicked(NativeMouseEvent e) {}

    // ─── SPAM ─────────────────────────────────────────────────────────────────

    private void startSpam() {
        if (robot == null || (task != null && !task.isDone())) return;
        task = scheduler.scheduleAtFixedRate(() -> {
            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
            robot.delay(10);
            robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        }, 0, THROW_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void stopSpam() {
        if (task != null) {
            task.cancel(false);
            task = null;
            if (robot != null) robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        }
    }
}