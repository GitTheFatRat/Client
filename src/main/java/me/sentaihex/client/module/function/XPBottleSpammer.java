package me.sentaihex.client.module.function;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import me.sentaihex.client.module.ClientModule;
import me.sentaihex.client.util.InputSimulator;

import java.util.concurrent.*;

public class XPBottleSpammer extends ClientModule implements NativeMouseListener {

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "XPBottleSpammer-Thread");
                t.setDaemon(true);
                return t;
            });

    private volatile boolean rightHeld = false;
    private final int DELAY_MS = 200; // Tăng lên 200ms cho an toàn với thực thể XP

    public XPBottleSpammer() {
        super("XP Bottle Spam", "Function", -1);
    }

    @Override
    public void onEnable() {
        GlobalScreen.addNativeMouseListener(this);
        System.out.println("[SentaiHex] XPBottleSpammer enabled");
    }

    @Override
    public void onDisable() {
        GlobalScreen.removeNativeMouseListener(this);
        rightHeld = false;
        System.out.println("[SentaiHex] XPBottleSpammer disabled");
    }

    @Override public void execute() {}

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        // ĐỔI THÀNH BUTTON3 (Chuột phải tiêu chuẩn trong JNativeHook)
        if (e.getButton() == NativeMouseEvent.BUTTON3) {
            if (!rightHeld) {
                rightHeld = true;
                startSpamLoop(); // Chạy vòng lặp đệ quy an toàn hơn FixedRate
            }
        }
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        if (e.getButton() == NativeMouseEvent.BUTTON3) {
            rightHeld = false;
        }
    }

    @Override public void nativeMouseClicked(NativeMouseEvent e) {}

    private void startSpamLoop() {
        if (!rightHeld) return;

        // Giả lập click chuột phải
        InputSimulator.rightClick();

        // Lên lịch cho cú click tiếp theo (Tạo khoảng trống cho CPU "thở")
        scheduler.schedule(this::startSpamLoop, DELAY_MS, TimeUnit.MILLISECONDS);
    }
}