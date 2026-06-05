package me.sentaihex.launcher;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import me.sentaihex.client.SentaiHex;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Launcher extends JFrame {

    // ── Status enum ───────────────────────────────────────────────────────────
    private enum StatusType { IDLE, SCANNING, OK, RUNNING, ERROR }

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG_DEEP     = new Color(4,   6,  10);
    private static final Color BG_MID      = new Color(9,  12,  18);
    private static final Color PANEL_BASE  = new Color(14,  18,  26);
    private static final Color PANEL_LIGHT = new Color(20,  26,  38);
    private static final Color CARD_BG     = new Color(16,  21,  32);
    private static final Color CARD_HOVER  = new Color(22,  28,  42);

    private static final Color BORDER_SUBTLE = new Color(38,  48,  68, 180);
    private static final Color BORDER_MID    = new Color(55,  70,  98, 220);
    private static final Color BORDER_GLOW   = new Color(80, 110, 160, 120);

    private static final Color ACCENT_A    = new Color( 32, 211, 190);
    private static final Color ACCENT_B    = new Color( 56, 162, 255);
    private static final Color ACCENT_DIM  = new Color( 32, 211, 190,  80);

    private static final Color OK_COLOR    = new Color( 52, 211, 153);
    private static final Color WARN_COLOR  = new Color(251, 191,  36);
    private static final Color ERR_COLOR   = new Color(248,  80, 100);
    private static final Color SCAN_COLOR  = new Color( 96, 165, 250);

    private static final Color TEXT_HI   = new Color(236, 242, 255);
    private static final Color TEXT_MID  = new Color(148, 163, 194);
    private static final Color TEXT_DIM  = new Color( 72,  88, 120);

    private static final int W      = 540;
    private static final int H      = 400;
    private static final int CORNER = 16;
    private static final String FONT = "Segoe UI";

    // ── State ─────────────────────────────────────────────────────────────────
    private JLabel    statusLabel;
    private JPanel    statusPill;
    private JComboBox<String> processCombo;
    private JButton   startBtn;
    private JButton   refreshBtn;
    private JPanel    dotGame;
    private JPanel    dotLaunch;
    private Point     dragOffset;

    private Timer     pulseTimer;
    private float     pulseAlpha = 0f;
    private boolean   pulseUp    = true;

    private final List<ProcessDetector.DetectedProcess> mcProcesses = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────────
    public Launcher() {
        setTitle("SentaiHex");
        setSize(W, H);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        setContentPane(buildRoot());

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), CORNER * 2, CORNER * 2));
            }
        });

        pulseTimer = new Timer(30, e -> {
            pulseAlpha += pulseUp ? 0.04f : -0.04f;
            if (pulseAlpha >= 1f) { pulseAlpha = 1f; pulseUp = false; }
            if (pulseAlpha <= 0f) { pulseAlpha = 0f; pulseUp = true;  }
            if (statusPill != null) statusPill.repaint();
        });

        scanProcesses();
    }

    // =========================================================================
    //  ROOT
    // =========================================================================
    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(BG_DEEP);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER * 2, CORNER * 2);
                paintGrid(g2);
                paintBlob(g2, -60, -60, 280, ACCENT_A, 0.06f);
                paintBlob(g2, getWidth() - 120, getHeight() - 80, 220, ACCENT_B, 0.05f);
                g2.setColor(BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER * 2, CORNER * 2);
                GradientPaint rim = new GradientPaint(0, 0, new Color(255, 255, 255, 18), getWidth(), 0, new Color(255, 255, 255, 4));
                g2.setPaint(rim);
                g2.drawLine(CORNER, 1, getWidth() - CORNER, 1);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildMain(),    BorderLayout.CENTER);
        return root;
    }

    private void paintGrid(Graphics2D g2) {
        g2.setColor(new Color(255, 255, 255, 4));
        g2.setStroke(new BasicStroke(0.5f));
        for (int x = 0; x < W; x += 28) g2.drawLine(x, 0, x, H);
        for (int y = 0; y < H; y += 28) g2.drawLine(0, y, W, y);
    }

    private void paintBlob(Graphics2D g2, int cx, int cy, int r, Color c, float alpha) {
        RadialGradientPaint rgp = new RadialGradientPaint(
                new Point2D.Float(cx + r / 2f, cy + r / 2f), r,
                new float[]{0f, 1f},
                new Color[]{new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(alpha * 255)), new Color(0, 0, 0, 0)}
        );
        g2.setPaint(rgp);
        g2.fillOval(cx, cy, r * 2, r * 2);
    }

    // =========================================================================
    //  SIDEBAR
    // =========================================================================
    private JPanel buildSidebar() {
        JPanel side = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(PANEL_BASE);
                g2.fillRoundRect(0, 0, getWidth() + CORNER, getHeight(), CORNER * 2, CORNER * 2);
                g2.fillRect(getWidth() - CORNER, 0, CORNER, getHeight());
                g2.setColor(BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(getWidth() - 1, 20, getWidth() - 1, getHeight() - 20);
                GradientPaint bar = new GradientPaint(0, 0, ACCENT_A, 0, getHeight(), ACCENT_B);
                g2.setPaint(bar);
                g2.fillRoundRect(0, 40, 3, getHeight() - 80, 3, 3);
                g2.dispose();
            }
        };
        side.setPreferredSize(new Dimension(116, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(24, 14, 20, 14));
        side.setOpaque(false);

        side.add(buildLogoMark());
        side.add(Box.createVerticalStrut(6));

        JLabel appName = new JLabel("SentaiHex");
        appName.setFont(new Font(FONT, Font.BOLD, 11));
        appName.setForeground(TEXT_HI);
        appName.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(appName);

        JLabel ver = new JLabel("v1.0.0");
        ver.setFont(new Font(FONT, Font.PLAIN, 9));
        ver.setForeground(TEXT_DIM);
        ver.setAlignmentX(Component.CENTER_ALIGNMENT);
        side.add(ver);

        side.add(Box.createVerticalStrut(32));
        side.add(buildDivider());
        side.add(Box.createVerticalStrut(20));

        dotGame   = buildDot(false);
        dotLaunch = buildDot(false);
        side.add(buildStepEntry("01", "Game",   dotGame));
        side.add(Box.createVerticalStrut(16));
        side.add(buildStepEntry("02", "Launch", dotLaunch));

        side.add(Box.createVerticalGlue());

        JButton close = buildIconBtn("x");
        close.setAlignmentX(Component.CENTER_ALIGNMENT);
        close.addActionListener(e -> {
            if (SentaiHex.INSTANCE != null) SentaiHex.INSTANCE.stop();
            System.exit(0);
        });
        side.add(close);

        enableDrag(side);
        return side;
    }

    private JPanel buildLogoMark() {
        JPanel logo = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                int s = Math.min(getWidth(), getHeight());
                g2.setColor(new Color(ACCENT_A.getRed(), ACCENT_A.getGreen(), ACCENT_A.getBlue(), 40));
                g2.fillRoundRect(0, 0, s, s, 14, 14);
                GradientPaint gp = new GradientPaint(0, 0, ACCENT_A, s, s, ACCENT_B);
                g2.setPaint(gp);
                g2.fillRoundRect(3, 3, s - 6, s - 6, 10, 10);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(3, 3, s - 7, s - 7, 10, 10);
                g2.setColor(BG_DEEP);
                g2.setFont(new Font(FONT, Font.BOLD, 22));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("S", (s - fm.stringWidth("S")) / 2, (s + fm.getAscent() - fm.getDescent()) / 2 - 1);
                g2.dispose();
            }
        };
        logo.setOpaque(false);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension d = new Dimension(48, 48);
        logo.setPreferredSize(d);
        logo.setMaximumSize(d);
        logo.setMinimumSize(d);
        return logo;
    }

    private JPanel buildDivider() {
        JPanel div = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                GradientPaint gp = new GradientPaint(0, 0, new Color(255, 255, 255, 0), getWidth() / 2f, 0, new Color(255, 255, 255, 28), true);
                g2.setPaint(gp);
                g2.fillRect(0, getHeight() / 2, getWidth(), 1);
                g2.dispose();
            }
        };
        div.setOpaque(false);
        div.setAlignmentX(Component.CENTER_ALIGNMENT);
        Dimension d = new Dimension(88, 8);
        div.setMaximumSize(d);
        div.setPreferredSize(d);
        return div;
    }

    private JPanel buildStepEntry(String num, String label, JPanel dot) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(90, 20));
        row.add(dot);
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel numLbl = new JLabel(num);
        numLbl.setFont(new Font(FONT, Font.BOLD, 8));
        numLbl.setForeground(TEXT_DIM);
        JLabel labelLbl = new JLabel(label);
        labelLbl.setFont(new Font(FONT, Font.PLAIN, 10));
        labelLbl.setForeground(TEXT_MID);
        text.add(numLbl);
        text.add(labelLbl);
        row.add(text);
        return row;
    }

    private JPanel buildDot(boolean active) {
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                boolean on = Boolean.TRUE.equals(getClientProperty("active"));
                int s = 8;
                int x = (getWidth() - s) / 2;
                int y = (getHeight() - s) / 2;
                if (on) {
                    g2.setColor(new Color(ACCENT_A.getRed(), ACCENT_A.getGreen(), ACCENT_A.getBlue(), 50));
                    g2.fillOval(x - 3, y - 3, s + 6, s + 6);
                    GradientPaint gp = new GradientPaint(x, y, ACCENT_A, x + s, y + s, ACCENT_B);
                    g2.setPaint(gp);
                    g2.fillOval(x, y, s, s);
                } else {
                    g2.setColor(BORDER_SUBTLE);
                    g2.fillOval(x, y, s, s);
                }
                g2.dispose();
            }
        };
        dot.putClientProperty("active", active);
        dot.setOpaque(false);
        Dimension d = new Dimension(14, 14);
        dot.setPreferredSize(d);
        dot.setMaximumSize(d);
        return dot;
    }

    private void setDotActive(JPanel dot, boolean active) {
        dot.putClientProperty("active", active);
        dot.repaint();
    }

    // =========================================================================
    //  MAIN PANEL
    // =========================================================================
    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(28, 24, 24, 28));
        main.add(buildHeader(),  BorderLayout.NORTH);
        main.add(buildCenter(),  BorderLayout.CENTER);
        main.add(buildActions(), BorderLayout.SOUTH);
        return main;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 22, 0));

        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(new Color(ACCENT_A.getRed(), ACCENT_A.getGreen(), ACCENT_A.getBlue(), 22));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.setColor(new Color(ACCENT_A.getRed(), ACCENT_A.getGreen(), ACCENT_A.getBlue(), 80));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setBorder(new EmptyBorder(3, 8, 3, 8));
        JLabel badgeLbl = new JLabel("CLIENT LOADER");
        badgeLbl.setFont(new Font(FONT, Font.BOLD, 8));
        badgeLbl.setForeground(ACCENT_A);
        badge.add(badgeLbl);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);
        badge.setMaximumSize(badge.getPreferredSize());

        JLabel title = new JLabel("Connect to Minecraft");
        title.setFont(new Font(FONT, Font.BOLD, 24));
        title.setForeground(TEXT_HI);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Select a running game process and hit Launch.");
        sub.setFont(new Font(FONT, Font.PLAIN, 12));
        sub.setForeground(TEXT_MID);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(badge);
        header.add(Box.createVerticalStrut(10));
        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(sub);

        enableDrag(header);
        return header;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(buildProcessSelector());
        center.add(Box.createVerticalStrut(14));
        center.add(buildStatusPill());
        center.add(Box.createVerticalGlue());
        return center;
    }

    // =========================================================================
    //  PROCESS SELECTOR (fully custom — no JComboBox rendered)
    // =========================================================================
    private int     selectedIndex = 0;
    private JLabel  selectedLabel;
    private JWindow dropdownWindow;

    private JPanel buildProcessSelector() {
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel("GAME PROCESS");
        lbl.setFont(new Font(FONT, Font.BOLD, 9));
        lbl.setForeground(TEXT_DIM);
        lbl.setBorder(new EmptyBorder(0, 2, 0, 0));

        // JComboBox used as data model only — never shown
        processCombo = new JComboBox<>();
        processCombo.setVisible(false);

        selectedLabel = new JLabel("Waiting...");
        selectedLabel.setFont(new Font(FONT, Font.PLAIN, 12));
        selectedLabel.setForeground(TEXT_DIM);

        JLabel chevron = new JLabel("v");
        chevron.setFont(new Font(FONT, Font.BOLD, 11));
        chevron.setForeground(TEXT_DIM);
        chevron.setBorder(new EmptyBorder(0, 0, 1, 0));

        JPanel trigger = new JPanel(new BorderLayout()) {
            boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                    @Override public void mousePressed(MouseEvent e) { toggleDropdown(); }
                });
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(hovered ? CARD_HOVER : CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(hovered ? BORDER_MID : BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(new Color(255, 255, 255, hovered ? 14 : 8));
                g2.drawLine(10, 1, getWidth() - 10, 1);
                g2.dispose();
            }
        };
        trigger.setOpaque(false);
        trigger.setBorder(new EmptyBorder(11, 14, 11, 14));
        trigger.add(selectedLabel, BorderLayout.CENTER);
        trigger.add(chevron,       BorderLayout.EAST);

        wrap.add(lbl,     BorderLayout.NORTH);
        wrap.add(trigger, BorderLayout.CENTER);
        return wrap;
    }

    private void toggleDropdown() {
        if (mcProcesses.isEmpty()) return;
        if (dropdownWindow != null && dropdownWindow.isVisible()) {
            dropdownWindow.dispose();
            dropdownWindow = null;
            return;
        }

        dropdownWindow = new JWindow(this);
        dropdownWindow.setBackground(new Color(0, 0, 0, 0));

        JPanel list = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(BORDER_MID);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        list.setBorder(new EmptyBorder(4, 4, 4, 4));

        for (int i = 0; i < mcProcesses.size(); i++) {
            final int idx = i;
            final String text = "PID " + mcProcesses.get(i).descriptor().id()
                    + "  -  " + mcProcesses.get(i).label();

            JPanel item = new JPanel(new BorderLayout()) {
                boolean hovered = false;
                {
                    addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                        @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                        @Override public void mousePressed(MouseEvent e) {
                            selectedIndex = idx;
                            selectedLabel.setText(text);
                            selectedLabel.setForeground(TEXT_HI);
                            dropdownWindow.dispose();
                            dropdownWindow = null;
                        }
                    });
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = aa(g);
                    boolean sel = idx == selectedIndex;
                    if (sel || hovered) {
                        Color fill = sel
                                ? new Color(ACCENT_A.getRed(), ACCENT_A.getGreen(), ACCENT_A.getBlue(), 30)
                                : new Color(255, 255, 255, 12);
                        g2.setColor(fill);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
                    }
                    g2.dispose();
                }
            };
            item.setOpaque(false);
            item.setBorder(new EmptyBorder(9, 12, 9, 12));

            JLabel itemLbl = new JLabel(text);
            itemLbl.setFont(new Font(FONT, Font.PLAIN, 12));
            itemLbl.setForeground(idx == selectedIndex ? TEXT_HI : TEXT_MID);
            item.add(itemLbl, BorderLayout.CENTER);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            list.add(item);

            if (i < mcProcesses.size() - 1) {
                JSeparator sep = new JSeparator();
                sep.setForeground(BORDER_SUBTLE);
                sep.setOpaque(false);
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                list.add(sep);
            }
        }

        dropdownWindow.setContentPane(list);
        dropdownWindow.pack();

        Component trigger = selectedLabel.getParent();
        Point loc = trigger.getLocationOnScreen();
        int w = Math.max(trigger.getWidth(), dropdownWindow.getWidth());
        dropdownWindow.setSize(w, dropdownWindow.getHeight());
        dropdownWindow.setLocation(loc.x, loc.y + trigger.getHeight() + 4);
        dropdownWindow.setVisible(true);

        dropdownWindow.addWindowFocusListener(new WindowAdapter() {
            @Override public void windowLostFocus(WindowEvent e) {
                if (dropdownWindow != null) {
                    dropdownWindow.dispose();
                    dropdownWindow = null;
                }
            }
        });
    }

    // =========================================================================
    //  STATUS PILL
    // =========================================================================
    private JPanel buildStatusPill() {
        statusPill = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                Color accent = (Color) getClientProperty("accent");
                if (accent == null) accent = TEXT_DIM;
                boolean scanning = Boolean.TRUE.equals(getClientProperty("scanning"));
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 14));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                        scanning ? (int)(40 + pulseAlpha * 80) : 50));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                if (scanning) {
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int)(pulseAlpha * 18)));
                    g2.fillRoundRect(-2, -2, getWidth() + 4, getHeight() + 4, getHeight() + 4, getHeight() + 4);
                }
                g2.dispose();
            }
        };
        statusPill.setOpaque(false);
        statusPill.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusPill.setBorder(new EmptyBorder(8, 14, 8, 18));

        JPanel indicator = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                Color accent = (Color) statusPill.getClientProperty("accent");
                if (accent == null) accent = TEXT_DIM;
                boolean scanning = Boolean.TRUE.equals(statusPill.getClientProperty("scanning"));
                int a = scanning ? (int)(120 + pulseAlpha * 135) : 220;
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), a));
                g2.fillOval(1, 1, 7, 7);
                g2.dispose();
            }
        };
        indicator.setOpaque(false);
        indicator.setPreferredSize(new Dimension(9, 9));
        statusPill.putClientProperty("indicator", indicator);

        statusLabel = new JLabel("Waiting for Minecraft...");
        statusLabel.setFont(new Font(FONT, Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_MID);

        statusPill.add(indicator);
        statusPill.add(statusLabel);
        statusPill.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return statusPill;
    }

    // =========================================================================
    //  ACTIONS
    // =========================================================================
    private JPanel buildActions() {
        JPanel actions = new JPanel(new BorderLayout(12, 0));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(16, 0, 0, 0));

        refreshBtn = buildOutlineBtn("Refresh");
        refreshBtn.setPreferredSize(new Dimension(120, 46));
        refreshBtn.addActionListener(e -> scanProcesses());

        startBtn = buildAccentBtn("Launch  ->");
        startBtn.addActionListener(e -> startClient());

        actions.add(refreshBtn, BorderLayout.WEST);
        actions.add(startBtn,   BorderLayout.CENTER);
        actions.add(buildFooterTag(), BorderLayout.SOUTH);
        return actions;
    }

    private JLabel buildFooterTag() {
        JLabel foot = new JLabel("Fabric  -  Forge  -  Lunar  -  Feather  -  Vanilla");
        foot.setFont(new Font(FONT, Font.PLAIN, 9));
        foot.setForeground(TEXT_DIM);
        foot.setBorder(new EmptyBorder(10, 2, 0, 0));
        return foot;
    }

    // =========================================================================
    //  BUTTONS
    // =========================================================================
    private JButton buildAccentBtn(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                boolean hover    = Boolean.TRUE.equals(getClientProperty("hover"));
                boolean disabled = !isEnabled();
                if (disabled) {
                    g2.setColor(new Color(30, 36, 50));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(BORDER_SUBTLE);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                } else {
                    Color c1 = hover ? ACCENT_A.brighter() : ACCENT_A;
                    Color c2 = hover ? ACCENT_B.brighter() : ACCENT_B;
                    GradientPaint gp = new GradientPaint(0, 0, c1, getWidth(), 0, c2);
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.setColor(new Color(255, 255, 255, hover ? 35 : 20));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 10, 10);
                    g2.fillRect(0, getHeight() / 4, getWidth(), getHeight() / 4);
                    g2.setColor(new Color(255, 255, 255, 25));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font(FONT, Font.BOLD, 13));
        btn.setForeground(new Color(4, 8, 14));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(13, 28, 13, 28));
        hoverFx(btn);
        return btn;
    }

    private JButton buildOutlineBtn(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                boolean hover = Boolean.TRUE.equals(getClientProperty("hover"));
                g2.setColor(hover ? CARD_HOVER : CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(hover ? BORDER_GLOW : BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font(FONT, Font.PLAIN, 12));
        btn.setForeground(TEXT_MID);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(13, 18, 13, 18));
        hoverFx(btn);
        return btn;
    }

    private JButton buildIconBtn(String icon) {
        JButton btn = new JButton(icon) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                if (Boolean.TRUE.equals(getClientProperty("hover"))) {
                    g2.setColor(new Color(ERR_COLOR.getRed(), ERR_COLOR.getGreen(), ERR_COLOR.getBlue(), 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font(FONT, Font.PLAIN, 13));
        btn.setForeground(TEXT_DIM);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 8, 5, 8));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setForeground(ERR_COLOR); btn.putClientProperty("hover", true);  btn.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { btn.setForeground(TEXT_DIM);  btn.putClientProperty("hover", false); btn.repaint(); }
        });
        return btn;
    }

    // =========================================================================
    //  STATUS
    // =========================================================================
    private void setStatus(String message, StatusType type) {
        Color accent;
        boolean scanning = false;
        switch (type) {
            case OK      -> { accent = OK_COLOR;   setDotActive(dotGame, true); }
            case RUNNING -> { accent = ACCENT_A;   setDotActive(dotGame, true); setDotActive(dotLaunch, true); }
            case ERROR   -> { accent = ERR_COLOR; }
            case SCANNING-> { accent = SCAN_COLOR; scanning = true; }
            default      -> { accent = TEXT_DIM; }
        }
        statusLabel.setText(message);
        statusLabel.setForeground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 220));
        statusPill.putClientProperty("accent",   accent);
        statusPill.putClientProperty("scanning", scanning);
        JPanel indicator = (JPanel) statusPill.getClientProperty("indicator");
        if (indicator != null) indicator.repaint();
        if (scanning) { if (!pulseTimer.isRunning()) pulseTimer.start(); }
        else          { pulseTimer.stop(); pulseAlpha = 0; }
        statusPill.repaint();
    }

    // =========================================================================
    //  LOGIC
    // =========================================================================
    private void scanProcesses() {
        processCombo.removeAllItems();
        mcProcesses.clear();
        selectedIndex = 0;
        setStatus("Scanning for game processes...", StatusType.SCANNING);
        setDotActive(dotGame,   false);
        setDotActive(dotLaunch, false);
        refreshBtn.setEnabled(false);
        if (selectedLabel != null) {
            selectedLabel.setText("Scanning...");
            selectedLabel.setForeground(TEXT_DIM);
        }

        new Thread(() -> {
            List<VirtualMachineDescriptor> all = VirtualMachine.list();
            for (VirtualMachineDescriptor vmd : all) {
                ProcessDetector.DetectedProcess p = ProcessDetector.classify(vmd);
                if (p.kind() == ProcessDetector.ProcessKind.MINECRAFT_GAME)
                    mcProcesses.add(p);
            }
            SwingUtilities.invokeLater(() -> {
                refreshBtn.setEnabled(true);
                if (mcProcesses.isEmpty()) {
                    processCombo.addItem("No game found");
                    if (selectedLabel != null) {
                        selectedLabel.setText("No game found - open Minecraft first");
                        selectedLabel.setForeground(ERR_COLOR);
                    }
                    setStatus("No Minecraft process detected", StatusType.ERROR);
                } else {
                    for (ProcessDetector.DetectedProcess p : mcProcesses)
                        processCombo.addItem("PID " + p.descriptor().id() + "  -  " + p.label());
                    selectedIndex = 0;
                    if (selectedLabel != null) {
                        selectedLabel.setText("PID " + mcProcesses.get(0).descriptor().id()
                                + "  -  " + mcProcesses.get(0).label());
                        selectedLabel.setForeground(TEXT_HI);
                    }
                    int n = mcProcesses.size();
                    setStatus(n + " game process" + (n > 1 ? "es" : "") + " found", StatusType.OK);
                    setDotActive(dotGame, true);
                }
            });
        }).start();
    }

    private void startClient() {
        if (SentaiHex.INSTANCE != null) {
            setStatus("Running - press INSERT to open the menu", StatusType.RUNNING);
            setDotActive(dotLaunch, true);
            return;
        }
        int idx = selectedIndex;
        if (mcProcesses.isEmpty() || idx < 0) {
            setStatus("Open Minecraft first, then refresh", StatusType.ERROR);
            return;
        }
        startBtn.setEnabled(false);
        refreshBtn.setEnabled(false);
        setStatus("Injecting client...", StatusType.SCANNING);

        ProcessDetector.DetectedProcess target = mcProcesses.get(idx);

        new Thread(() -> {
            String attachNote = tryAttachCompanionAgent(target.descriptor());
            try {
                SentaiHex.INSTANCE = new SentaiHex();
                SentaiHex.INSTANCE.start();
                SwingUtilities.invokeLater(() -> {
                    String msg = attachNote.isBlank()
                            ? "Running - press INSERT to open the menu"
                            : "Running - agent linked - press INSERT to open the menu";
                    setStatus(msg, StatusType.RUNNING);
                    startBtn.setText("Running");
                    setDotActive(dotLaunch, true);
                    showBanner("Client injected successfully" + attachNote);
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
            return " - agent linked";
        } catch (Exception ex) {
            System.out.println("[SentaiHex] Agent attach skipped: " + ex.getMessage());
            return "";
        }
    }

    // ── Toast banner ──────────────────────────────────────────────────────────
    private void showBanner(String message) {
        JWindow banner = new JWindow(this);
        banner.setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(new Color(10, 14, 22, 245));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                GradientPaint gp = new GradientPaint(0, 0, ACCENT_A, getWidth(), 0, ACCENT_B);
                g2.setPaint(gp);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cy = getHeight() / 2;
                g2.drawLine(10, cy, 15, cy + 5);
                g2.drawLine(15, cy + 5, 24, cy - 5);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(11, 34, 11, 18));

        JLabel lbl = new JLabel(message);
        lbl.setFont(new Font(FONT, Font.PLAIN, 12));
        lbl.setForeground(TEXT_HI);
        panel.add(lbl);

        banner.setContentPane(panel);
        banner.pack();
        banner.setSize(Math.max(banner.getWidth(), 320), banner.getHeight());
        Point loc = getLocationOnScreen();
        banner.setLocation(loc.x + (getWidth() - banner.getWidth()) / 2, loc.y + getHeight() + 10);
        banner.setVisible(true);
        new Timer(3200, e -> banner.dispose()).start();
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================
    private static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
        return g2;
    }

    private void hoverFx(JButton btn) {
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.putClientProperty("hover", true);  btn.repaint(); }
            @Override public void mouseExited(MouseEvent e)  { btn.putClientProperty("hover", false); btn.repaint(); }
        });
    }

    private void enableDrag(JComponent c) {
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

    // ── Entry point ───────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new Launcher().setVisible(true);
        });
    }
}