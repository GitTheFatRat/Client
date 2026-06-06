package me.sentaihex.client.module.function;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import me.sentaihex.client.module.ClientModule;
import me.sentaihex.client.util.MinecraftAccess;
import me.sentaihex.client.util.InputSimulator;

import java.util.concurrent.*;
import java.util.concurrent.ThreadLocalRandom;

public class XPBottleSpammer extends ClientModule implements NativeMouseListener {

    /** Random interval between 12-15 CPS to avoid ghost item desync. */
    private static final int CPS_MIN_MS = 1000 / 15;
    private static final int CPS_MAX_MS = 1000 / 12;

    /** Ignore JNativeHook release events briefly after each synthetic input. */
    private static final int SUPPRESS_RELEASE_MS = 60;

    /** Delayed check when a release was ignored -- catches real user release. */
    private static final int VERIFY_RELEASE_MS = 45;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "XPBottleSpammer-Thread");
                t.setDaemon(true);
                return t;
            });

    private volatile boolean spamActive = false;
    private volatile boolean userHolding = false;
    private volatile long suppressReleaseUntil = 0;
    private volatile ScheduledFuture<?> spamTask;

    // ThreadLocalRandom for better performance and less contention
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public XPBottleSpammer() {
        super("XP Bottle Spam", "Function", -1);
    }

    @Override
    public void onEnable() {
        GlobalScreen.addNativeMouseListener(this);
        System.out.println("[SentaiHex] XPBottleSpammer enabled (~17 CPS)");
    }

    @Override
    public void onDisable() {
        stopSpamLoop();
        GlobalScreen.removeNativeMouseListener(this);
        System.out.println("[SentaiHex] XPBottleSpammer disabled");
    }

    @Override public void execute() {}

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        if (!isEnabled() || !isRightClick(e)) return;
        if (!canSpamHeldItem()) {
            logSkipReason();
            return;
        }
        userHolding = true;
        startSpamLoop();
    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {
        if (!isRightClick(e)) return;

        long now = System.currentTimeMillis();
        if (now < suppressReleaseUntil) {
            scheduler.schedule(this::verifyUserReleased, VERIFY_RELEASE_MS, TimeUnit.MILLISECONDS);
            return;
        }

        userHolding = false;
        stopSpamLoop();
    }

    @Override public void nativeMouseClicked(NativeMouseEvent e) {}

    private static boolean isRightClick(NativeMouseEvent e) {
        int btn = e.getButton();
        return btn == NativeMouseEvent.BUTTON2 || btn == NativeMouseEvent.BUTTON3;
    }

    private void startSpamLoop() {
        if (spamActive) return;
        spamActive = true;
        spamTick();
    }

    private void spamTick() {
        if (!spamActive || !isEnabled() || !userHolding || !canSpamHeldItem()) {
            stopSpamLoop();
            return;
        }

        InputSimulator.xpBottleUseTick();
        suppressReleaseUntil = System.currentTimeMillis() + SUPPRESS_RELEASE_MS;

        // Use ThreadLocalRandom instead of Math.random() for better performance
        int interval = CPS_MIN_MS + random.nextInt(CPS_MAX_MS - CPS_MIN_MS + 1);
        spamTask = scheduler.schedule(this::spamTick, interval, TimeUnit.MILLISECONDS);
    }

    private void verifyUserReleased() {
        if (!spamActive) return;
        if (!InputSimulator.isRightMouseHeld()) {
            userHolding = false;
            stopSpamLoop();
        }
    }

    /** Check via MinecraftAccess (same JVM as Minecraft) instead of file-based agent. */
    private static boolean canSpamHeldItem() {
        if (!MinecraftAccess.initMc()) return true;
        return MinecraftAccess.isHoldingExperienceBottle();
    }

    private static void logSkipReason() {
        System.out.println("[SentaiHex] XP spam: need experience bottle in main hand (held: "
                + MinecraftAccess.getHeldItemRegistryId() + ")");
    }

    private void stopSpamLoop() {
        spamActive = false;
        userHolding = false;
        if (spamTask != null) {
            spamTask.cancel(false);
            spamTask = null;
        }
    }
}