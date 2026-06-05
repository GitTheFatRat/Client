package me.sentaihex.agent;

import me.sentaihex.client.util.HeldItemBridge;
import me.sentaihex.client.util.MinecraftAccess;

import java.lang.instrument.Instrumentation;

/**
 * Companion agent inside the Minecraft JVM — polls held item for XP bottle gating.
 */
public class Agent {

    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[SentaiHex] Companion agent attached to Minecraft JVM");
        HeldItemBridge.writeHeldItemId("init");

        Thread thread = new Thread(() -> {
            try {
                for (int attempt = 0; attempt < 120; attempt++) {
                    Thread.sleep(250);
                    if (MinecraftAccess.initMc(inst)) {
                        System.out.println("[SentaiHex] Held-item polling active");
                        pollHeldItemLoop();
                        return;
                    }
                }
                System.out.println("[SentaiHex] Could not init MC access — XP bottle filter unavailable");
                HeldItemBridge.writeHeldItemId("");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                System.err.println("[SentaiHex] Agent error: " + e.getMessage());
            }
        }, "SentaiHex-Agent");

        thread.setDaemon(true);
        thread.start();
    }

    public static void premain(String args, Instrumentation inst) {
        agentmain(args, inst);
    }

    private static void pollHeldItemLoop() throws InterruptedException {
        while (true) {
            String id = MinecraftAccess.getHeldItemRegistryId();
            HeldItemBridge.writeHeldItemId(id);
            Thread.sleep(50);
        }
    }
}
