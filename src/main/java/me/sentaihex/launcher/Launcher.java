package me.sentaihex.launcher;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import me.sentaihex.client.SentaiHex;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Launcher extends JFrame {

    private enum StatusType { IDLE, SCANNING, OK, RUNNING, ERROR }

    private JLabel statusText;
    private JPanel statusPill;
    private JComboBox<String> processCombo;
    private JButton startBtn;
    private JButton refreshBtn;
    private JPanel stepGame;
    private JPanel stepLaunch;
    private Point dragOffset;

    private final List<ProcessDetector.DetectedProcess> mcProcesses = new ArrayList<>();

    private static final int RADIUS = 10;
    private static final String FONT = "Segoe UI";

    private static final Color BG          = new Color(8, 10, 14);
    private static final Color SIDEBAR     = new Color(12, 15, 20);
    private static final Color SURFACE     = new Color(18, 22, 30);
    private static final Color SURFACE_ALT = new Color(24, 30, 40);
    private static final Color BORDER      = new Color(42, 52, 68);
    private static final Color MINT        = new Color(0, 229, 160);
    private static final Color CYAN        = new Color(0, 212, 255);
    private static final Color TEXT        = new Color(232, 237, 245);
    private static final Color MUTED       = new Color(110, 122, 145);
    private static final Color DANGER      = new Color(255, 92, 122);
    private static final Color WARN        = new Color(255, 184, 77);
    private static final Color OK          = new Color(0, 229, 160);

    public Launcher() {
        setTitle("SentaiHex Launcher");
        setSize(520, 380);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        setContentPane(buildRoot());

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), RADIUS, RADIUS));
            }
        });

        scanProcesses();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS, RADIUS);
                paintGrid(g2);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildMain(), BorderLayout.CENTER);
        return root;
    }

    private void paintGrid(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 6));
        for (int x = 0; x < getWidth(); x += 24) g2.drawLine(x, 0, x, getHeight());
        for (int y = 0; y < getHeight(); y += 24) g2.drawLine(0, y, getWidth(), y);
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SIDEBAR);
                g2.fillRoundRect(0, 0, getWidth() + RADIUS, getHeight(), RADIUS, RADIUS);
                g2.fillRect(RADIUS / 2, 0, getWidth(), getHeight());

                GradientPaint stripe = new GradientPaint(0, 0, MINT, 0, getHeight(), CYAN);
                g2.setPaint(stripe);
                g2.fillRect(0, 0, 3, getHeight());
                g2.dispose();
            }
        };
        side.setPreferredSize(new Dimension(108, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(18, 16, 18, 12));
        side.setOpaque(false);

        JPanel logo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, MINT, getWidth(), getHeight(), CYAN);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BG);
                g2.setFont(new Font(FONT, Font.BOLD, 20));
                FontMetrics fm = g2.getFontMetrics();
                String s = "S";
                g2.drawString(s, (getWidth() - fm.stringWidth(s)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        logo.setOpaque(false);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setMaximumSize(new Dimension(48, 48));
        logo.setPreferredSize(new Dimension(48, 48));

        JLabel ver = new JLabel("v1.0");
        ver.setFont(new Font(FONT, Font.PLAIN, 10));
        ver.setForeground(MUTED);
        ver.setAlignmentX(Component.CENTER_ALIGNMENT);

        side.add(logo);
        side.add(Box.createVerticalStrut(20));
        side.add(ver);
        side.add(Box.createVerticalStrut(28));
        side.add(buildStep("1", "Game", stepGame = stepDot(false)));
        side.add(Box.createVerticalStrut(14));
        side.add(buildStep("2", "Launch", stepLaunch = stepDot(false)));
        side.add(Box.createVerticalGlue());

        JButton close = iconButton("×", DANGER);
        close.setAlignmentX(Component.CENTER_ALIGNMENT);
        close.addActionListener(e -> {
            if (SentaiHex.INSTANCE != null) SentaiHex.INSTANCE.stop();
            System.exit(0);
        });
        side.add(close);

        enableWindowDrag(side);
        return side;
    }

    private JPanel buildStep(String num, String label, JPanel dot) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        row.setMaximumSize(new Dimension(90, 24));

        JLabel n = new JLabel(num);
        n.setFont(new Font(FONT, Font.BOLD, 10));
        n.setForeground(MUTED);

        JLabel l = new JLabel(label);
        l.setFont(new Font(FONT, Font.PLAIN, 10));
        l.setForeground(MUTED);

        row.add(dot);
        row.add(l);
        return row;
    }

    private JPanel stepDot(boolean active) {
        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean on = Boolean.TRUE.equals(getClientProperty("active"));
                Color c = on ? MINT : BORDER;
                g2.setColor(c);
                g2.fill(new Ellipse2D.Double(3, 3, 8, 8));
                if (on) {
                    g2.setColor(new Color(MINT.getRed(), MINT.getGreen(), MINT.getBlue(), 60));
                    g2.fill(new Ellipse2D.Double(0, 0, 14, 14));
                }
                g2.dispose();
            }
        };
        dot.putClientProperty("active", active);
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(14, 14));
        return dot;
    }

    private void setStepActive(JPanel dot, boolean active) {
        dot.putClientProperty("active", active);
        dot.repaint();
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(22, 8, 22, 22));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel heading = new JLabel("Connect to Minecraft");
        heading.setFont(new Font(FONT, Font.BOLD, 22));
        heading.setForeground(TEXT);

        JLabel sub = new JLabel("Select your game process and launch the client.");
        sub.setFont(new Font(FONT, Font.PLAIN, 12));
        sub.setForeground(MUTED);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        titles.add(heading);
        titles.add(Box.createVerticalStrut(4));
        titles.add(sub);

        top.add(titles, BorderLayout.CENTER);
        enableWindowDrag(top);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(22, 0, 18, 0));

        center.add(buildProcessField());
        center.add(Box.createVerticalStrut(14));
        center.add(buildStatusPill());

        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(4, 0, 0, 0));

        refreshBtn = outlineButton("Refresh");
        refreshBtn.addActionListener(e -> scanProcesses());

        startBtn = launchButton("Launch  →");
        startBtn.addActionListener(e -> startClient());

        actions.add(refreshBtn, BorderLayout.WEST);
        actions.add(startBtn, BorderLayout.CENTER);

        main.add(top, BorderLayout.NORTH);
        main.add(center, BorderLayout.CENTER);
        main.add(actions, BorderLayout.SOUTH);
        main.add(buildFooter(), BorderLayout.PAGE_END);

        return main;
    }

    private JPanel buildProcessField() {
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        JLabel lbl = new JLabel("GAME PROCESS");
        lbl.setFont(new Font(FONT, Font.BOLD, 10));
        lbl.setForeground(MUTED);

        processCombo = new JComboBox<>();
        processCombo.setFont(new Font(FONT, Font.PLAIN, 12));
        processCombo.setForeground(TEXT);
        processCombo.setBackground(SURFACE);
        processCombo.setBorder(new EmptyBorder(0, 0, 0, 0));
        processCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        processCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                c.setFont(new Font(FONT, Font.PLAIN, 12));
                c.setBorder(new EmptyBorder(10, 14, 10, 14));
                if (isSelected) {
                    c.setBackground(new Color(0, 229, 160, 35));
                    c.setForeground(TEXT);
                } else {
                    c.setBackground(SURFACE_ALT);
                    c.setForeground(MUTED);
                }
                return c;
            }
        });

        JPanel comboShell = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        comboShell.setOpaque(false);
        comboShell.setBorder(new EmptyBorder(2, 2, 2, 2));
        comboShell.add(processCombo, BorderLayout.CENTER);

        wrap.add(lbl, BorderLayout.NORTH);
        wrap.add(comboShell, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildStatusPill() {
        statusPill = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = (Color) getClientProperty("pillBg");
                if (bg == null) bg = SURFACE;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        statusPill.setOpaque(false);
        statusPill.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusPill.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        statusPill.setBorder(new EmptyBorder(8, 14, 8, 14));
        statusPill.putClientProperty("pillBg", SURFACE);

        statusText = new JLabel("Waiting for Minecraft…");
        statusText.setFont(new Font(FONT, Font.PLAIN, 12));
        statusText.setForeground(MUTED);
        statusPill.add(statusText);
        return statusPill;
    }

    private JLabel buildFooter() {
        JLabel foot = new JLabel("Prism  ·  Modrinth  ·  Feather  ·  Fabric  ·  Forge");
        foot.setFont(new Font(FONT, Font.PLAIN, 10));
        foot.setForeground(new Color(65, 75, 95));
        foot.setBorder(new EmptyBorder(12, 0, 0, 0));
        return foot;
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private JButton launchButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = Boolean.TRUE.equals(getClientProperty("hover"));
                boolean disabled = !isEnabled();
                if (disabled) {
                    g2.setColor(new Color(40, 48, 60));
                } else {
                    GradientPaint gp = new GradientPaint(
                            0, 0, hover ? new Color(30, 255, 190) : MINT,
                            getWidth(), 0, hover ? new Color(80, 230, 255) : CYAN
                    );
                    g2.setPaint(gp);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font(FONT, Font.BOLD, 14));
        btn.setForeground(new Color(8, 12, 18));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(13, 24, 13, 24));
        addHover(btn);
        return btn;
    }

    private JButton outlineButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean hover = Boolean.TRUE.equals(getClientProperty("hover"));
                g2.setColor(hover ? SURFACE_ALT : SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(hover ? MINT : BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font(FONT, Font.PLAIN, 13));
        btn.setForeground(TEXT);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(13, 18, 13, 18));
        btn.setPreferredSize(new Dimension(110, 0));
        addHover(btn);
        return btn;
    }

    private JButton iconButton(String text, Color hoverColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(FONT, Font.PLAIN, 16));
        btn.setForeground(MUTED);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setForeground(hoverColor); }
            @Override public void mouseExited(MouseEvent e)  { btn.setForeground(MUTED); }
        });
        return btn;
    }

    private void addHover(JButton btn) {
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.putClientProperty("hover", Boolean.TRUE);
                btn.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.putClientProperty("hover", Boolean.FALSE);
                btn.repaint();
            }
        });
    }

    private void enableWindowDrag(JComponent c) {
        MouseAdapter drag = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { dragOffset = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (dragOffset == null) return;
                Point s = e.getLocationOnScreen();
                setLocation(s.x - dragOffset.x, s.y - dragOffset.y);
            }
        };
        c.addMouseListener(drag);
        c.addMouseMotionListener(drag);
    }

    // ── Status ────────────────────────────────────────────────────────────────

    private void setStatus(String message, StatusType type) {
        statusText.setText(message);
        Color fg;
        Color pillBg;
        switch (type) {
            case OK, RUNNING -> {
                fg = OK;
                pillBg = new Color(0, 229, 160, 18);
                setStepActive(stepGame, true);
                setStepActive(stepLaunch, type == StatusType.RUNNING);
            }
            case ERROR -> {
                fg = DANGER;
                pillBg = new Color(255, 92, 122, 18);
            }
            case SCANNING -> {
                fg = WARN;
                pillBg = new Color(255, 184, 77, 18);
            }
            default -> {
                fg = MUTED;
                pillBg = SURFACE;
            }
        }
        statusText.setForeground(fg);
        statusPill.putClientProperty("pillBg", pillBg);
        statusPill.repaint();
    }

    // ── Logic (unchanged behaviour) ───────────────────────────────────────────

    private void scanProcesses() {
        processCombo.removeAllItems();
        mcProcesses.clear();
        setStatus("Scanning for game processes…", StatusType.SCANNING);
        setStepActive(stepGame, false);
        setStepActive(stepLaunch, false);
        refreshBtn.setEnabled(false);

        new Thread(() -> {
            List<VirtualMachineDescriptor> all = VirtualMachine.list();
            for (VirtualMachineDescriptor vmd : all) {
                ProcessDetector.DetectedProcess detected = ProcessDetector.classify(vmd);
                if (detected.kind() == ProcessDetector.ProcessKind.MINECRAFT_GAME) {
                    mcProcesses.add(detected);
                }
            }

            SwingUtilities.invokeLater(() -> {
                refreshBtn.setEnabled(true);
                if (mcProcesses.isEmpty()) {
                    processCombo.addItem("No game found — start Minecraft first");
                    setStatus("No Minecraft process detected", StatusType.ERROR);
                } else {
                    for (ProcessDetector.DetectedProcess detected : mcProcesses) {
                        processCombo.addItem("PID " + detected.descriptor().id() + "  ·  " + detected.label());
                    }
                    setStatus(mcProcesses.size() + " game process"
                            + (mcProcesses.size() > 1 ? "es" : "") + " ready", StatusType.OK);
                    setStepActive(stepGame, true);
                }
            });
        }).start();
    }

    private void startClient() {
        if (SentaiHex.INSTANCE != null) {
            setStatus("Running — press INSERT in-game", StatusType.RUNNING);
            setStepActive(stepLaunch, true);
            return;
        }

        int idx = processCombo.getSelectedIndex();
        if (mcProcesses.isEmpty() || idx < 0) {
            setStatus("Start Minecraft, then refresh", StatusType.ERROR);
            return;
        }

        startBtn.setEnabled(false);
        refreshBtn.setEnabled(false);
        setStatus("Launching client…", StatusType.SCANNING);

        ProcessDetector.DetectedProcess target = mcProcesses.get(idx);

        new Thread(() -> {
            String attachNote = tryAttachCompanionAgent(target.descriptor());

            try {
                SentaiHex.INSTANCE = new SentaiHex();
                SentaiHex.INSTANCE.start();

                SwingUtilities.invokeLater(() -> {
                    String msg = attachNote.isBlank()
                            ? "Running — INSERT for menu (XP spam needs agent)"
                            : "Running · agent linked · INSERT for menu";
                    setStatus(msg, StatusType.RUNNING);
                    startBtn.setText("Running");
                    setStepActive(stepLaunch, true);
                    showBanner("Client started" + attachNote);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setStatus("Failed: " + ex.getMessage(), StatusType.ERROR);
                    startBtn.setEnabled(true);
                    refreshBtn.setEnabled(true);
                });
            }
        }).start();
    }

    private String tryAttachCompanionAgent(VirtualMachineDescriptor target) {
        try {
            String jarPath = new File(
                    Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            ).getAbsolutePath();

            VirtualMachine vm = VirtualMachine.attach(target.id());
            vm.loadAgent(jarPath);
            vm.detach();
            System.out.println("[SentaiHex] Companion agent attached to PID " + target.id());
            return " · agent linked";
        } catch (Exception ex) {
            System.out.println("[SentaiHex] Agent attach skipped: " + ex.getMessage());
            return "";
        }
    }

    private void showBanner(String message) {
        JWindow banner = new JWindow(this);
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(12, 16, 22, 250));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                GradientPaint gp = new GradientPaint(0, 0, MINT, getWidth(), 0, CYAN);
                g2.setPaint(gp);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

                Path2D check = new Path2D.Float();
                check.moveTo(12, getHeight() / 2f);
                check.lineTo(18, getHeight() / 2f + 6);
                check.lineTo(28, getHeight() / 2f - 6);
                g2.draw(check);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(12, 40, 12, 16));

        JLabel lbl = new JLabel(message);
        lbl.setFont(new Font(FONT, Font.PLAIN, 12));
        lbl.setForeground(TEXT);
        panel.add(lbl, BorderLayout.CENTER);

        banner.setContentPane(panel);
        banner.pack();
        banner.setSize(Math.max(banner.getWidth(), 300), banner.getHeight());
        Point loc = getLocationOnScreen();
        banner.setLocation(loc.x + (getWidth() - banner.getWidth()) / 2, loc.y + getHeight() + 8);
        banner.setVisible(true);
        new Timer(3000, e -> banner.dispose()).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new Launcher().setVisible(true);
        });
    }
}
