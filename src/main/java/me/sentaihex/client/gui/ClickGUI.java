package me.sentaihex.client.gui;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import me.sentaihex.client.SentaiHex;
import me.sentaihex.client.module.ClientModule;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class ClickGUI extends JFrame implements NativeKeyListener {

    // === GLASS THEME ===
    private static final Color GLASS_BG     = new Color(10, 10, 18, 210);
    private static final Color GLASS_CARD   = new Color(255, 255, 255, 18);
    private static final Color GLASS_BORDER = new Color(255, 255, 255, 40);
    private static final Color GLASS_HOVER  = new Color(255, 255, 255, 30);
    private static final Color ACCENT       = new Color(130, 180, 255);
    private static final Color TEXT_MAIN    = new Color(240, 240, 245);
    private static final Color TEXT_MUTED   = new Color(140, 145, 160);

    private ClientModule listeningModule = null;
    private String listeningSlot = null;
    private JButton listeningBtn = null;
    private long guiOpenedAt = 0;

    private final JPanel mainContent;

    public ClickGUI() {
        setTitle("SentaiHex Menu");
        setSize(780, 520);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);

        GlobalScreen.addNativeKeyListener(this);

        JPanel bgPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GLASS_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(GLASS_BORDER);
                g2.setStroke(new BasicStroke(1.0f));
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        bgPanel.setOpaque(false);
        bgPanel.setBorder(new EmptyBorder(15, 20, 20, 20));

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(false);
        JLabel titleLabel = new JLabel("SENTAIHEX  |  UTILITY CLIENT");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(ACCENT);
        titleBar.add(titleLabel, BorderLayout.WEST);

        JButton closeBtn = new JButton("X");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        closeBtn.setForeground(TEXT_MUTED);
        closeBtn.setBorderPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(Color.RED); }
            @Override
            public void mouseExited(MouseEvent e) { closeBtn.setForeground(TEXT_MUTED); }
        });
        closeBtn.addActionListener(e -> hideGUI());
        titleBar.add(closeBtn, BorderLayout.EAST);

        bgPanel.add(titleBar, BorderLayout.NORTH);

        mainContent = new JPanel(new GridBagLayout());
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(15, 0, 0, 0));

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        bgPanel.add(scrollPane, BorderLayout.CENTER);
        setContentPane(bgPanel);

        final Point[] dragPt = new Point[1];
        titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { dragPt[0] = e.getPoint(); }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragPt[0] != null) {
                    Point curr = getLocation();
                    setLocation(curr.x + e.getX() - dragPt[0].x, curr.y + e.getY() - dragPt[0].y);
                }
            }
        });

        buildUI();
    }

    private void buildUI() {
        mainContent.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;

        int row = 0;
        int col = 0;

        for (ClientModule mod : SentaiHex.INSTANCE.moduleManager.getModules()) {
            JPanel card = createModuleCard(mod);
            gbc.gridx = col;
            gbc.gridy = row;
            mainContent.add(card, gbc);

            col++;
            if (col > 1) {
                col = 0;
                row++;
            }
        }
        mainContent.revalidate();
        mainContent.repaint();
    }

    private JPanel createModuleCard(ClientModule mod) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GLASS_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(mod.isEnabled() ? ACCENT : GLASS_BORDER);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel nameLabel = new JLabel(mod.getName());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(mod.isEnabled() ? ACCENT : TEXT_MAIN);
        card.add(nameLabel, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);

        String keyText = mod.getKeybind() == 0 ? "NONE" : NativeKeyEvent.getKeyText(mod.getKeybind());
        JButton bindBtn = createGlassButton(formatKey(keyText));
        bindBtn.addActionListener(e -> {
            listeningModule = mod;
            listeningSlot = "mainBind";
            listeningBtn = bindBtn;
            bindBtn.setText("...");
            bindBtn.setForeground(Color.YELLOW);
        });
        controls.add(bindBtn);

        setupSpecialModuleSlots(mod, controls);

        JButton toggleBtn = createGlassButton(mod.isEnabled() ? "ON" : "OFF");
        toggleBtn.setForeground(mod.isEnabled() ? ACCENT : TEXT_MUTED);
        toggleBtn.addActionListener(e -> {
            mod.toggle();
            toggleBtn.setText(mod.isEnabled() ? "ON" : "OFF");
            toggleBtn.setForeground(mod.isEnabled() ? ACCENT : TEXT_MUTED);
            nameLabel.setForeground(mod.isEnabled() ? ACCENT : TEXT_MAIN);
            card.repaint();
            SentaiHex.INSTANCE.configManager.save();
        });
        controls.add(toggleBtn);

        card.add(controls, BorderLayout.EAST);
        return card;
    }

    private void setupSpecialModuleSlots(ClientModule mod, JPanel controls) {
        String className = mod.getClass().getSimpleName();

        if ("AutoReply".equals(className)) {
            setupSlotBind(mod, "slotKey", controls);
        } else if ("FastLever".equals(className)) {
            setupSlotBind(mod, "leverKey", controls);
        } else if ("LeftClicker".equals(className)) {
            setupSlotBind(mod, "clickKey", controls);
        } else if ("AutoTotemMacro".equals(className)) {
            setupSlotBind(mod, "totemKey", controls);
        } else if ("ObsidianMacro".equals(className)) {
            setupSlotBind(mod, "obsidianKey", controls);
            setupSlotBind(mod, "gappleKey", controls);
        } else if ("PearlMacro".equals(className)) {
            setupSlotBind(mod, "pearlKey", controls);
        } else if ("PotMacro".equals(className)) {
            setupSlotBind(mod, "potKey", controls);
        } else if ("RefillMacro".equals(className)) {
            setupSlotBind(mod, "refillKey", controls);
        } else if ("WebMacro".equals(className)) {
            setupSlotBind(mod, "webKey", controls);
        } else if ("XPBottleSpammer".equals(className)) {
            setupSlotBind(mod, "xpKey", controls);
        }
    }

    private void setupSlotBind(ClientModule mod, String slot, JPanel controls) {
        int code = getSlotKey(mod, slot);
        String txt = code == 0 ? "NONE" : NativeKeyEvent.getKeyText(code);
        JButton btn = createGlassButton(formatKey(txt));
        btn.addActionListener(e -> {
            listeningModule = mod;
            listeningSlot = slot;
            listeningBtn = btn;
            btn.setText("...");
            btn.setForeground(Color.YELLOW);
        });
        controls.add(btn);
    }

    private int getSlotKey(ClientModule mod, String slot) {
        try { return mod.getClass().getField(slot).getInt(mod); } catch (Exception e) { return 0; }
    }

    private void setSlotKey(ClientModule mod, String slot, int code) {
        try { mod.getClass().getField(slot).setInt(mod, code); } catch (Exception ignored) {}
    }

    private JButton createGlassButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? GLASS_HOVER : GLASS_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 6, 6));
                g2.setColor(GLASS_BORDER);
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, 6, 6));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(TEXT_MAIN);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(65, 26));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    public void showGUI() {
        guiOpenedAt = System.currentTimeMillis();
        buildUI();
        setVisible(true);
        toFront();
        requestFocus();
    }

    public void hideGUI() {
        setVisible(false);
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        int code = e.getKeyCode();

        if (listeningModule != null && listeningSlot != null && listeningBtn != null) {
            if (System.currentTimeMillis() - guiOpenedAt < 300) return;
            if (code == NativeKeyEvent.VC_INSERT) return;

            final ClientModule mod  = listeningModule;
            final String       slot = listeningSlot;
            final JButton      btn  = listeningBtn;
            listeningModule = null;
            listeningSlot   = null;
            listeningBtn    = null;

            final String keyTxt = formatKey(NativeKeyEvent.getKeyText(code));
            SwingUtilities.invokeLater(() -> {
                if (slot.equals("mainBind")) mod.setKeybind(code);
                else setSlotKey(mod, slot, code);
                btn.setText(keyTxt);
                btn.setForeground(ACCENT);
                btn.repaint();
                SentaiHex.INSTANCE.configManager.save();
            });
            return;
        }

        if (code == NativeKeyEvent.VC_INSERT) {
            SwingUtilities.invokeLater(() -> {
                if (isVisible()) hideGUI(); else showGUI();
            });
        }
    }

    @Override public void nativeKeyReleased(NativeKeyEvent e) {}
    @Override public void nativeKeyTyped(NativeKeyEvent e) {}

    private String formatKey(String raw) {
        return raw.length() > 6 ? raw.substring(0, 5) + "." : raw;
    }
}