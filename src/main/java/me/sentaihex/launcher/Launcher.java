package me.sentaihex.launcher;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Launcher extends JFrame {

    private JLabel statusLabel;
    private JComboBox<String> processCombo;
    private JButton injectBtn;
    private final List<VirtualMachineDescriptor> mcProcesses = new ArrayList<>();

    private static final Color BG       = new Color(10, 14, 20);
    private static final Color PANEL_BG = new Color(15, 20, 30);
    private static final Color ACCENT   = new Color(0, 255, 153);
    private static final Color RED      = new Color(255, 45, 107);
    private static final Color TEXT     = new Color(200, 240, 224);
    private static final Color MUTED    = new Color(80, 120, 100);

    public Launcher() {
        setTitle("SentaiHex Launcher");
        setSize(460, 340);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(BorderFactory.createLineBorder(ACCENT, 1));

        // Title bar
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(PANEL_BG);
        titleBar.setBorder(new EmptyBorder(10, 14, 10, 10));

        JLabel title = new JLabel("⚡ SENTAIHEX v1.0");
        title.setFont(new Font("Courier New", Font.BOLD, 15));
        title.setForeground(ACCENT);

        JButton closeBtn = new JButton("✕");
        closeBtn.setForeground(RED);
        closeBtn.setBackground(PANEL_BG);
        closeBtn.setBorder(new EmptyBorder(0, 10, 0, 0));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> System.exit(0));

        titleBar.add(title, BorderLayout.WEST);
        titleBar.add(closeBtn, BorderLayout.EAST);

        MouseAdapter drag = new MouseAdapter() {
            Point start;
            public void mousePressed(MouseEvent e) { start = e.getPoint(); }
            public void mouseDragged(MouseEvent e) {
                Point loc = getLocation();
                setLocation(loc.x + e.getX() - start.x, loc.y + e.getY() - start.y);
            }
        };
        titleBar.addMouseListener(drag);
        titleBar.addMouseMotionListener(drag);

        // Content
        JPanel content = new JPanel();
        content.setBackground(BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 28, 24, 28));

        statusLabel = new JLabel("// READY - Chon process Minecraft");
        statusLabel.setFont(new Font("Courier New", Font.PLAIN, 11));
        statusLabel.setForeground(MUTED);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel selectLabel = new JLabel("TARGET PROCESS:");
        selectLabel.setFont(new Font("Courier New", Font.BOLD, 11));
        selectLabel.setForeground(ACCENT);
        selectLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectLabel.setBorder(new EmptyBorder(18, 0, 6, 0));

        processCombo = new JComboBox<>();
        processCombo.setBackground(PANEL_BG);
        processCombo.setForeground(TEXT);
        processCombo.setFont(new Font("Courier New", Font.PLAIN, 12));
        processCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        processCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        btnRow.setBackground(BG);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setBorder(new EmptyBorder(18, 0, 0, 0));

        JButton refreshBtn = makeBtn("[ REFRESH ]", MUTED);
        refreshBtn.addActionListener(e -> scanProcesses());

        injectBtn = makeBtn("[ INJECT ]", ACCENT);
        injectBtn.setFont(new Font("Courier New", Font.BOLD, 13));
        injectBtn.addActionListener(e -> inject());

        btnRow.add(refreshBtn);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(injectBtn);

        JLabel info = new JLabel("// Ho tro: Lunar, Feather, Legacy, Modrinth, Prism, Badlion | 1.20.4 - 1.21.1");
        info.setFont(new Font("Courier New", Font.PLAIN, 10));
        info.setForeground(new Color(40, 70, 55));
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        info.setBorder(new EmptyBorder(18, 0, 0, 0));

        content.add(statusLabel);
        content.add(selectLabel);
        content.add(processCombo);
        content.add(btnRow);
        content.add(info);

        root.add(titleBar, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
        scanProcesses();
    }

    private JButton makeBtn(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Courier New", Font.PLAIN, 12));
        btn.setForeground(color);
        btn.setBackground(PANEL_BG);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                new EmptyBorder(7, 16, 7, 16)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(20, 30, 25)); }
            public void mouseExited(MouseEvent e) { btn.setBackground(PANEL_BG); }
        });
        return btn;
    }

    private void scanProcesses() {
        processCombo.removeAllItems();
        mcProcesses.clear();
        statusLabel.setText("// Dang scan processes...");
        statusLabel.setForeground(ACCENT);

        new Thread(() -> {
            List<VirtualMachineDescriptor> all = VirtualMachine.list();
            for (VirtualMachineDescriptor vmd : all) {
                String name = vmd.displayName().toLowerCase();
                // ✅ Fix: thêm mcProcesses.add(vmd) đúng chỗ
                if (name.contains("minecraft")
                        // Lunar Client
                        || name.contains("lunar")
                        || name.contains("com.moonsworth")
                        || name.contains("lunarclient")
                        // Feather Client
                        || name.contains("feather")
                        || name.contains("gg.essential")
                        // Legacy Launcher (официальный лаунчер / tlauncher / legacy)
                        || name.contains("legacy")
                        || name.contains("legacylauncher")
                        || name.contains("tlauncher")
                        // Modrinth App
                        || name.contains("modrinth")
                        || name.contains("theseus")
                        // Prism Launcher
                        || name.contains("prism")
                        || name.contains("prismlauncher")
                        || name.contains("org.prismlauncher")
                        // Badlion
                        || name.contains("badlion")
                        || name.contains("digitalingot")
                        || name.contains("net.digitalingot")
                        // LabyMod
                        || name.contains("labymod")
                        // PVP Legacy
                        || name.contains("pvplegacy")
                        || name.contains("proxiedstart")
                        || name.contains("rustextension")
                        // Generic fallback
                        || name.contains("net.minecraft")) {
                    mcProcesses.add(vmd); // ✅ BUG FIX: dòng này bị thiếu trong code cũ!
                }
            }
            SwingUtilities.invokeLater(() -> {
                if (mcProcesses.isEmpty()) {
                    processCombo.addItem("-- Khong tim thay Minecraft --");
                    statusLabel.setText("// Khong tim thay! Mo Minecraft truoc.");
                    statusLabel.setForeground(RED);
                } else {
                    for (VirtualMachineDescriptor vmd : mcProcesses) {
                        String label = "[PID:" + vmd.id() + "] " + vmd.displayName();
                        if (label.length() > 60) label = label.substring(0, 60) + "...";
                        processCombo.addItem(label);
                    }
                    statusLabel.setText("// Tim thay " + mcProcesses.size() + " process [1.20.4-1.21.1]");
                    statusLabel.setForeground(ACCENT);
                }
            });
        }).start();
    }

    private void inject() {
        int idx = processCombo.getSelectedIndex();
        if (mcProcesses.isEmpty() || idx < 0) {
            statusLabel.setText("// Khong co process de inject!");
            statusLabel.setForeground(RED);
            return;
        }

        injectBtn.setEnabled(false);
        statusLabel.setText("// Dang inject...");
        statusLabel.setForeground(ACCENT);

        VirtualMachineDescriptor target = mcProcesses.get(idx);

        new Thread(() -> {
            try {
                String jarPath = new File(
                        Launcher.class.getProtectionDomain()
                                .getCodeSource().getLocation().toURI()
                ).getAbsolutePath();

                VirtualMachine vm = VirtualMachine.attach(target.id());
                vm.loadAgent(jarPath);
                vm.detach();

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("// INJECTED!");
                    statusLabel.setForeground(ACCENT);
                    injectBtn.setText("[ INJECTED ]");
                    injectBtn.setEnabled(false);
                    showInjectedPopup();
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("// Loi: " + ex.getMessage());
                    statusLabel.setForeground(RED);
                    injectBtn.setEnabled(true);
                });
            }
        }).start();
    }

    private void showInjectedPopup() {
        JDialog dialog = new JDialog(this, false);
        dialog.setUndecorated(true);
        dialog.setSize(320, 80);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createLineBorder(ACCENT, 1));

        JLabel msg = new JLabel("INJECTED OK [1.20.4-1.21.1] - Nhan INSERT de mo GUI", SwingConstants.CENTER);
        msg.setFont(new Font("Courier New", Font.BOLD, 11));
        msg.setForeground(ACCENT);

        panel.add(msg, BorderLayout.CENTER);
        dialog.setContentPane(panel);
        dialog.setVisible(true);
        new Timer(3000, e -> dialog.dispose()).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Launcher().setVisible(true));
    }
}