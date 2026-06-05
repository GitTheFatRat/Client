package me.sentaihex.client.module.function;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;
import me.sentaihex.client.module.ClientModule;
import me.sentaihex.client.util.HeldItemBridge;
import me.sentaihex.client.util.InputSimulator;

import java.util.concurrent.*;

public class XPBottleSpammer extends ClientModule implements NativeMouseListener {

    /** Target ~17 CPS (within 15–19 range for XP bottle throw spam). */
    private static final int CLICK_INTERVAL_MS = 1000 / 17;

    /** Ignore JNativeHook release events briefly after each synthetic input. */
    private static final int SUPPRESS_RELEASE_MS = 30;

    /** Delayed check when a release was ignored — catches real user release. */
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

    public XPBottleSpammer() {
        super("XP Bottle Spam", "Function", -1);
    }

    @Override
    public void onEnable() {
        GlobalScreen.addNativeMouseListener(this);
        System.out.println("[SentaiHex] XPBottleSpammer enabled (XP bottles only, ~17 CPS)");
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
            // Synthetic UP from our spam tick — verify later in case this was a real release
            scheduler.schedule(this::verifyUserReleased, VERIFY_RELEASE_MS, TimeUnit.MILLISECONDS);
            return;
        }

        userHolding = false;
        stopSpamLoop();
    }

    @Override public void nativeMouseClicked(NativeMouseEvent e) {}

    /** JNativeHook: BUTTON2 on Windows/Linux, BUTTON3 on some setups. */
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

        // UP then DOWN: register one throw, end in held state for the next tick
        InputSimulator.xpBottleUseTick();
        suppressReleaseUntil = System.currentTimeMillis() + SUPPRESS_RELEASE_MS;

        spamTask = scheduler.schedule(this::spamTick, CLICK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * After an ignored release event, confirm whether the user actually let go.
     * Synthetic ticks leave the button down; a real release will not.
     */
    private void verifyUserReleased() {
        if (!spamActive) return;
        if (!InputSimulator.isRightMouseHeld()) {
            userHolding = false;
            stopSpamLoop();
        }
    }

    /** Only spam when the agent reports an experience bottle in the main hand. */
    private static boolean canSpamHeldItem() {
        if (!HeldItemBridge.isAgentActive()) return false;
        if (HeldItemBridge.isAgentInitializing()) return false;
        return HeldItemBridge.isHoldingExperienceBottle();
    }

    private static void logSkipReason() {
        if (!HeldItemBridge.isAgentActive()) {
            System.out.println("[SentaiHex] XP spam: agent not linked — launch client with game running");
            return;
        }
        if (HeldItemBridge.isAgentInitializing()) {
            System.out.println("[SentaiHex] XP spam: waiting for item detection…");
            return;
        }
        System.out.println("[SentaiHex] XP spam: need experience bottle in main hand (held: "
                + HeldItemBridge.readHeldItemId() + ")");
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
