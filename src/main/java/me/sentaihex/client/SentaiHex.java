package me.sentaihex.client;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import me.sentaihex.client.config.ConfigManager;
import me.sentaihex.client.gui.ClickGUI;
import me.sentaihex.client.module.ModuleManager;

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
            System.err.println("[SentaiHex] Lỗi hook: " + e.getMessage());
            return;
        }

        configManager = new ConfigManager();
        moduleManager = new ModuleManager();
        configManager.load();

        // Khởi động GUI trên EDT
        SwingUtilities.invokeLater(() -> {
            gui = new ClickGUI();
            System.out.println("[SentaiHex] Ready! Nhấn INSERT để mở GUI");
        });
    }

    public void stop() {
        try {
            configManager.save();
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
        }
    }
}