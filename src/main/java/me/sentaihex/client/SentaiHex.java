package me.sentaihex.client;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import me.sentaihex.client.config.ConfigManager;
import me.sentaihex.client.gui.ClickGUI;
import me.sentaihex.client.module.ModuleManager;
import me.sentaihex.client.module.ClientModule;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SentaiHex {

    public static SentaiHex INSTANCE;
    public static final String NAME = "SentaiHex";
    public static final String VERSION = "1.0.0";

    public ModuleManager moduleManager;
    public ConfigManager configManager;
    public ClickGUI gui;

    public void start() {
        System.out.println("[SentaiHex] Starting...");

        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            System.err.println("[SentaiHex] Hook error: " + e.getMessage());
            return;
        }

        configManager = new ConfigManager();
        moduleManager = new ModuleManager();
        configManager.load();

        // Add shutdown hook to save config and clean up
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[SentaiHex] Shutting down...");
            if (configManager != null) {
                configManager.save();
            }
            if (moduleManager != null) {
                for (ClientModule m : moduleManager.getModules()) {
                    if (m.isEnabled()) {
                        try {
                            m.onDisable();
                        } catch (Exception ignored) {}
                    }
                }
            }
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException ignored) {}
            System.out.println("[SentaiHex] Shutdown complete.");
        }, "SentaiHex-Shutdown"));

        // Start GUI on EDT
        SwingUtilities.invokeLater(() -> {
            gui = new ClickGUI();
            System.out.println("[SentaiHex] Ready! Press INSERT to open GUI");
        });
    }

    public void stop() {
        try {
            if (configManager != null) {
                configManager.save();
            }
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
        }
    }
}