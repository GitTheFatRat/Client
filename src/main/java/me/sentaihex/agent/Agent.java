package me.sentaihex.agent;

import me.sentaihex.client.SentaiHex;
import java.lang.instrument.Instrumentation;

public class Agent {

    public static void agentmain(String args, Instrumentation inst) {
        System.out.println("[SentaiHex] Agent injected!");

        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(3000);

                // Fix HeadlessException: Lunar/Prism set java.awt.headless=true
                // phải unset trước khi khởi tạo bất kỳ Swing component nào
                System.setProperty("java.awt.headless", "false");

                // Toolkit cần được reset sau khi đổi headless property
                // dùng reflection để clear cached toolkit instance
                try {
                    var toolkitClass = Class.forName("java.awt.Toolkit");
                    var field = toolkitClass.getDeclaredField("toolkit");
                    field.setAccessible(true);
                    field.set(null, null);
                    System.out.println("[SentaiHex] Toolkit reset OK");
                } catch (Exception e) {
                    System.out.println("[SentaiHex] Toolkit reset skipped: " + e.getMessage());
                }

                // Tìm MC classloader
                ClassLoader mcClassLoader = findMinecraftClassLoader(inst);
                if (mcClassLoader != null) {
                    System.out.println("[SentaiHex] Found Minecraft classloader: " + mcClassLoader.getClass().getName());
                    Thread.currentThread().setContextClassLoader(mcClassLoader);
                } else {
                    System.out.println("[SentaiHex] Using default classloader");
                }

                SentaiHex.INSTANCE = new SentaiHex();
                SentaiHex.INSTANCE.start();

            } catch (Exception e) {
                System.err.println("[SentaiHex] Error starting: " + e.getMessage());
                e.printStackTrace();
            }
        }, "SentaiHex-Main");

        thread.setDaemon(true);
        thread.setContextClassLoader(Thread.currentThread().getContextClassLoader());
        thread.start();
    }

    public static void premain(String args, Instrumentation inst) {
        agentmain(args, inst);
    }

    private static ClassLoader findMinecraftClassLoader(Instrumentation inst) {
        String[] mcClasses = {
                "net.minecraft.client.Minecraft",
                "net.minecraft.client.main.Main",
                "net.minecraft.SharedConstants",
                "net.minecraft.util.SharedConstants",
        };

        Class<?>[] allLoaded = inst.getAllLoadedClasses();

        for (Class<?> cls : allLoaded) {
            String name = cls.getName();
            for (String target : mcClasses) {
                if (name.equals(target)) {
                    ClassLoader cl = cls.getClassLoader();
                    if (cl != null) return cl;
                }
            }
        }

        for (Class<?> cls : allLoaded) {
            if (cls.getName().startsWith("net.minecraft.")) {
                ClassLoader cl = cls.getClassLoader();
                if (cl != null) return cl;
            }
        }

        for (Thread t : Thread.getAllStackTraces().keySet()) {
            String tName = t.getName().toLowerCase();
            if (tName.contains("client") || tName.contains("main") || tName.equals("render thread")) {
                ClassLoader cl = t.getContextClassLoader();
                if (cl != null && !cl.getClass().getName().contains("sun.misc")) {
                    return cl;
                }
            }
        }

        return null;
    }
}