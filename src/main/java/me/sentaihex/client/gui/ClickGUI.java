package me.sentaihex.client.gui;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import me.sentaihex.client.SentaiHex;
import me.sentaihex.client.module.ClientModule;

import me.sentaihex.client.module.macros.*;
import me.sentaihex.client.module.function.XPBottleSpammer;
import me.sentaihex.client.module.function.TriggerBot;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

public class ClickGUI extends JFrame implements NativeKeyListener {

    // --- THEME ---
    private static final Color BG_DEEP      = new Color(8,   10,  18);
    private static final Color BG_SURFACE   = new Color(14,  17,  28);
    private static final Color BG_ELEVATED  = new Color(22,  26,  42);
    private static final Color BORDER_DIM   = new Color(255, 255, 255, 22);
    private static final Color ACCENT       = new Color(100, 160, 255);
    private static final Color FUNC_ACCENT  = new Color(255, 160,  80);
    private static final Color TEXT_MAIN    = new Color(230, 235, 255);
    private static final Color TEXT_MUTED   = new Color(110, 125, 165);
    private static final Color TEXT_LABEL   = new Color(70,  85,  130);
    private static final Color TOGGLE_ON_M  = new Color(70,  200, 120);
    private static final Color TOGGLE_ON_F  = new Color(255, 160,  80);
    private static final Color TOGGLE_OFF   = new Color(30,  36,  58);
    private static final Color DANGER       = new Color(255,  75, 100);

    private static final int    CARD_R  = 12;
    private static final int    WIN_R   = 16;
    private static final String FONT    = "Segoe UI"; // NOSONAR

    // --- STATE ---
    private ClientModule  listeningModule  = null;
    private String        listeningSlot    = null;
    private JButton       listeningBtn     = null;
    private Point         dragPoint        = null;
    private volatile long guiOpenedAt      = 0;

    // GUI keybind (default INSERT)
    private int     guiKeybind       = NativeKeyEvent.VC_INSERT;
    private boolean listeningGuiBind = false;
    private JButton guiBindBtn       = null;

    // --- CONSTRUCTOR ---
    public ClickGUI() {
        setTitle("SentaiHex");
        setSize(1060, 560);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setAlwaysOnTop(true);
        if (getContentPane() instanceof JComponent jc) jc.setOpaque(false);
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), WIN_R * 2, WIN_R * 2));
            }
        });
        setContentPane(buildRoot());
        GlobalScreen.addNativeKeyListener(this);
    }

    // --- ROOT ---
    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_DEEP);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), WIN_R * 2, WIN_R * 2);
                g2.setColor(BORDER_DIM);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, WIN_R * 2, WIN_R * 2);
                g2.setColor(new Color(255, 255, 255, 7));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, WIN_R * 2, WIN_R * 2);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildContent(), BorderLayout.CENTER);
        return root;
    }

    // --- HEADER ---
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(11, 14, 24));
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + WIN_R, WIN_R * 2, WIN_R * 2);
                // bottom accent line
                GradientPaint gp = new GradientPaint(0, 0, new Color(80, 140, 255, 140), getWidth(), 0, new Color(255, 120, 60, 50));
                g2.setPaint(gp);
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(1060, 56));
        header.setBorder(new EmptyBorder(0, 20, 0, 14));

        // Left: title
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel title = new JLabel("SentaiHex");
        title.setFont(new Font(FONT, Font.BOLD, 16));
        title.setForeground(TEXT_MAIN);
        JLabel ver = new JLabel("v1.0");
        ver.setFont(new Font(FONT, Font.PLAIN, 11));
        ver.setForeground(TEXT_LABEL);
        left.add(title); left.add(ver);
        header.add(left, BorderLayout.WEST);

        // Right: open key bind + close
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JLabel bindLbl = new JLabel("Open key:");
        bindLbl.setFont(new Font(FONT, Font.PLAIN, 11));
        bindLbl.setForeground(TEXT_MUTED);

        guiBindBtn = new JButton(formatKey(NativeKeyEvent.getKeyText(guiKeybind))) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(listeningGuiBind ? new Color(255, 75, 100, 30) : new Color(255, 255, 255, 10));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(listeningGuiBind ? DANGER : new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 80));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        guiBindBtn.setFont(new Font(FONT, Font.BOLD, 11));
        guiBindBtn.setForeground(ACCENT);
        guiBindBtn.setContentAreaFilled(false);
        guiBindBtn.setBorderPainted(false);
        guiBindBtn.setFocusPainted(false);
        guiBindBtn.setPreferredSize(new Dimension(66, 24));
        guiBindBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        guiBindBtn.addActionListener(e -> {
            listeningGuiBind = true;
            guiBindBtn.setText("...");
            guiBindBtn.setForeground(DANGER);
            guiBindBtn.repaint();
        });

        JButton closeBtn = new JButton("X");
        closeBtn.setFont(new Font(FONT, Font.PLAIN, 13));
        closeBtn.setForeground(TEXT_MUTED);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setPreferredSize(new Dimension(28, 28));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { closeBtn.setForeground(DANGER); }
            @Override public void mouseExited(MouseEvent e)  { closeBtn.setForeground(TEXT_MUTED); }
        });
        closeBtn.addActionListener(e -> hideGUI());

        right.add(bindLbl); right.add(guiBindBtn); right.add(closeBtn);
        header.add(right, BorderLayout.EAST);

        // drag
        header.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { dragPoint = e.getPoint(); }
        });
        header.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (dragPoint != null) {
                    Point p = e.getLocationOnScreen();
                    setLocation(p.x - dragPoint.x, p.y - dragPoint.y);
                }
            }
        });
        return header;
    }

    // --- CONTENT: macros row top, functions row bottom ---
    private JPanel buildContent() {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(14, 16, 16, 16));

        java.util.List<ClientModule> macros    = SentaiHex.INSTANCE.moduleManager.getByCategory("Macro");
        java.util.List<ClientModule> functions = SentaiHex.INSTANCE.moduleManager.getByCategory("Function");

        if (!macros.isEmpty()) {
            wrapper.add(buildSectionLabel("MACROS", ACCENT));
            wrapper.add(Box.createVerticalStrut(8));
            wrapper.add(buildHorizontalRow(macros, true));
        }

        if (!functions.isEmpty()) {
            wrapper.add(Box.createVerticalStrut(16));
            wrapper.add(buildSectionLabel("FUNCTIONS", FUNC_ACCENT));
            wrapper.add(Box.createVerticalStrut(8));
            wrapper.add(buildHorizontalRow(functions, false));
        }

        wrapper.add(Box.createVerticalGlue());
        return wrapper;
    }

    private JPanel buildHorizontalRow(java.util.List<ClientModule> modules, boolean isMacro) {
        JPanel row = new JPanel(new GridLayout(1, modules.size(), 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (ClientModule m : modules)
            row.add(isMacro ? buildMacroCard(m) : buildFunctionCard(m));
        return row;
    }

    // --- SECTION LABEL ---
    private JLabel buildSectionLabel(String text, Color accent) {
        JLabel label = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillRoundRect(0, (getHeight() - 12) / 2, 3, 12, 3, 3);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        label.setFont(new Font(FONT, Font.BOLD, 10));
        label.setForeground(accent.brighter());
        label.setBorder(new EmptyBorder(0, 8, 0, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    // --- CARD BASE ---
    private JPanel buildCard(boolean isMacro) {
        Color bg     = BG_SURFACE;
        Color bgHov  = BG_ELEVATED;
        Color accent = isMacro ? ACCENT : FUNC_ACCENT;

        JPanel card = new JPanel() {
            boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    @Override public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? bgHov : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CARD_R, CARD_R);
                g2.setColor(hovered
                        ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 55)
                        : BORDER_DIM);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CARD_R, CARD_R);
                // left accent bar
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), hovered ? 210 : 130));
                g2.fillRoundRect(0, 10, 3, getHeight() - 20, 3, 3);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 15, 12, 13));
        return card;
    }

    // --- MACRO CARD ---
    private JPanel buildMacroCard(ClientModule m) {
        JPanel card = buildCard(true);
        String displayName = switch (m.getName()) {
            case "Anchor Bomb"                    -> "Respawn Anchor";
            case "TNT Cart"                       -> "TNT Cart";
            case "TNT Cart (Fire Bow)"            -> "TNT Cart (Fire Bow)";
            case "TNT Cart (Flint + Crossbow)"    -> "Flint + Crossbow";
            case "Mace Tech 1 (Pearl+Wind)"       -> "Pearl + Wind";
            case "Mace Tech 2 (Stun Slam)"        -> "Stun Slam";
            default                               -> m.getName();
        };
        card.add(buildTopRow(m, displayName, true), BorderLayout.NORTH);
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 0, 0, 0));
        buildSlots(m, body);
        body.add(Box.createVerticalStrut(6));
        body.add(buildDelayRow(m, false));
        if (m instanceof FlintCrossbowTNTCartMacro fc) {
            body.add(Box.createVerticalStrut(6));
            body.add(buildAimRow("Aim (flint)", fc.getAimFlint(),
                    v -> { fc.setAimFlint(v); SentaiHex.INSTANCE.configManager.save(); }));
            body.add(Box.createVerticalStrut(6));
            body.add(buildAimRow("Aim (bow)", fc.getAimBow(),
                    v -> { fc.setAimBow(v); SentaiHex.INSTANCE.configManager.save(); }));
        }
        body.add(Box.createVerticalStrut(6));
        body.add(buildBindRow(m, false));
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // --- FUNCTION CARD ---
    private JPanel buildFunctionCard(ClientModule m) {
        JPanel card = buildCard(false);
        card.add(buildTopRow(m, m.getName(), false), BorderLayout.NORTH);
        if (!isToggleOnly(m)) {
            card.add(buildFunctionBody(m), BorderLayout.CENTER);
        }
        return card;
    }

    private JPanel buildFunctionBody(ClientModule m) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 0, 0, 0));
        if (m instanceof TriggerBot) {
            body.add(buildSlotRow(m, "slot1", "Weapon Slot Filter"));
            body.add(Box.createVerticalStrut(4));
        }
        body.add(buildDelayRow(m, true));
        body.add(Box.createVerticalStrut(6));
        body.add(buildBindRow(m, true));
        return body;
    }

    private boolean isToggleOnly(ClientModule m) {
        return m instanceof XPBottleSpammer;
    }

    // --- TOP ROW ---
    private JPanel buildTopRow(ClientModule m, String displayName, boolean isMacro) {
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(new Font(FONT, Font.BOLD, 13));
        nameLabel.setForeground(TEXT_MAIN);
        topRow.add(nameLabel, BorderLayout.WEST);
        topRow.add(buildToggle(m, isMacro), BorderLayout.EAST);
        return topRow;
    }

    // --- TOGGLE ---
    private JComponent buildToggle(ClientModule m, boolean isMacro) {
        Color onColor = isMacro ? TOGGLE_ON_M : TOGGLE_ON_F;
        JButton toggle = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean on = m.isEnabled();
                g2.setColor(on ? onColor : TOGGLE_OFF);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(on ? new Color(onColor.getRed(), onColor.getGreen(), onColor.getBlue(), 90)
                        : new Color(255, 255, 255, 12));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                int knob = getHeight() - 4;
                int x    = on ? getWidth() - knob - 2 : 2;
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fillOval(x + 1, 3, knob, knob);
                g2.setColor(Color.WHITE);
                g2.fillOval(x, 2, knob, knob);
                g2.dispose();
            }
        };
        toggle.setPreferredSize(new Dimension(42, 22));
        toggle.setContentAreaFilled(false);
        toggle.setBorderPainted(false);
        toggle.setFocusPainted(false);
        toggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggle.addActionListener(e -> { m.toggle(); toggle.repaint(); SentaiHex.INSTANCE.configManager.save(); });
        m.addPropertyChangeListener(evt -> {
            if ("enabled".equals(evt.getPropertyName())) SwingUtilities.invokeLater(toggle::repaint);
        });
        return toggle;
    }

    // --- SLOTS ---
    private void buildSlots(ClientModule m, JPanel body) {
        if (m instanceof AnchorMacro) {
            body.add(buildSlotRow(m, "slot1", "Anchor"));
            body.add(Box.createVerticalStrut(4));
            body.add(buildSlotRow(m, "slot2", "Glowstone"));
            body.add(Box.createVerticalStrut(4));
            body.add(buildSlotRow(m, "slot3", "Totem"));
        } else if (m instanceof TNTCartMacro) {
            body.add(buildSlotRow(m, "slot1", "Rail"));
            body.add(Box.createVerticalStrut(4));
            body.add(buildSlotRow(m, "slot2", "Cart"));
            body.add(Box.createVerticalStrut(4));
            body.add(buildSlotRow(m, "slot3", "Crossbow"));
        } else if (m instanceof FireBowTNTCartMacro) {
            body.add(buildSlotRow(m, "slot1", "Fire Bow"));
            body.add(Box.createVerticalStrut(4));
            body.add(buildSlotRow(m, "slot2", "Rail"));
            body.add(Box.createVerticalStrut(4));
            body.add(buildSlotRow(m, "slot3", "Cart"));
        } else if (m instanceof FlintCrossbowTNTCartMacro) {
            body.add(buildSlotRow(m, "slot1", "Rail"));
            body.add(Box.createVerticalStrut(4));
            body.add(buildSlotRow(m, "slot2", "Cart"));
            body.add(Box.createVerticalStrut(4));
            body.add(buildSlotRow(m, "slot3", "Flint & Steel"));
            body.add(Box.createVerticalStrut(4));
            body.add(buildSlotRow(m, "slot4", "Crossbow"));
        } else if (m instanceof MaceTech1) {
            body.add(buildSlotRow(m, "slot1", "Pearl"));
            body.add(Box.createVerticalStrut(4));
            body.add(buildSlotRow(m, "slot2", "Wind Charge"));
        }
    }

    // --- SLOT ROW ---
    private JPanel buildSlotRow(ClientModule module, String slotName, String label) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(FONT, Font.PLAIN, 12));
        lbl.setForeground(TEXT_MUTED);
        row.add(lbl, BorderLayout.WEST);
        boolean isFn = module.getCategory().equals("Function");
        row.add(buildBindBtn(module, slotName, isFn), BorderLayout.EAST);
        return row;
    }

    // --- DELAY ROW ---
    private JPanel buildDelayRow(ClientModule m, boolean isFn) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel("Delay");
        lbl.setFont(new Font(FONT, Font.PLAIN, 12));
        lbl.setForeground(TEXT_MUTED);
        row.add(lbl, BorderLayout.WEST);

        Color accentColor = isFn ? FUNC_ACCENT : ACCENT;
        JTextField field  = buildDelayField(m, accentColor);
        Runnable   apply  = buildDelayApply(m, field, accentColor);
        field.addActionListener(e -> apply.run());
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { apply.run(); }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        JLabel ms = new JLabel("ms");
        ms.setFont(new Font(FONT, Font.PLAIN, 11));
        ms.setForeground(TEXT_LABEL);
        right.add(field); right.add(ms);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JTextField buildDelayField(ClientModule m, Color accentColor) {
        JTextField field = new JTextField(String.valueOf(m.getGlobalDelay()), 4) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 60));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.setFont(new Font(FONT, Font.BOLD, 12));
        field.setForeground(accentColor);
        field.setCaretColor(accentColor);
        field.setBorder(new EmptyBorder(2, 6, 2, 6));
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setPreferredSize(new Dimension(52, 24));
        return field;
    }

    private Runnable buildDelayApply(ClientModule m, JTextField field, Color accentColor) {
        return () -> {
            try {
                int val = Integer.parseInt(field.getText().trim());
                m.setDelay(val);
                field.setText(String.valueOf(m.getGlobalDelay()));
                field.setForeground(accentColor);
                SentaiHex.INSTANCE.configManager.save();
            } catch (NumberFormatException ex) {
                field.setForeground(DANGER);
            }
        };
    }

    // --- AIM ROW ---
    private JPanel buildAimRow(String labelText, int initialMs, java.util.function.IntConsumer onApply) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font(FONT, Font.PLAIN, 12));
        lbl.setForeground(TEXT_MUTED);
        row.add(lbl, BorderLayout.WEST);

        JTextField field = new JTextField(String.valueOf(initialMs), 4) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 60));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        field.setOpaque(false);
        field.setFont(new Font(FONT, Font.BOLD, 12));
        field.setForeground(ACCENT);
        field.setCaretColor(ACCENT);
        field.setBorder(new EmptyBorder(2, 6, 2, 6));
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setPreferredSize(new Dimension(52, 24));

        Runnable apply = () -> {
            try {
                int val = Math.max(0, Integer.parseInt(field.getText().trim()));
                onApply.accept(val);
                field.setText(String.valueOf(val));
                field.setForeground(ACCENT);
            } catch (NumberFormatException ex) {
                field.setForeground(DANGER);
            }
        };
        field.addActionListener(e -> apply.run());
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { apply.run(); }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        JLabel ms = new JLabel("ms");
        ms.setFont(new Font(FONT, Font.PLAIN, 11));
        ms.setForeground(TEXT_LABEL);
        right.add(field); right.add(ms);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    // --- BIND ROW ---
    private JPanel buildBindRow(ClientModule m, boolean isFn) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(isFn ? "Toggle" : "Trigger");
        lbl.setFont(new Font(FONT, Font.PLAIN, 12));
        lbl.setForeground(TEXT_MUTED);
        row.add(lbl, BorderLayout.WEST);
        row.add(buildBindBtn(m, "mainBind", isFn), BorderLayout.EAST);
        return row;
    }

    // --- BIND BUTTON ---
    private JButton buildBindBtn(ClientModule module, String slotName, boolean isFn) {
        int    cur     = slotName.equals("mainBind") ? module.getKeybind() : getSlotKey(module, slotName);
        String txt     = cur == -1 ? "-" : formatKey(NativeKeyEvent.getKeyText(cur));
        Color  fgColor = isFn ? FUNC_ACCENT : ACCENT;

        JButton btn = new JButton(txt) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean listening = this == listeningBtn;
                g2.setColor(listening ? new Color(255, 75, 100, 30) : new Color(255, 255, 255, 10));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(listening ? DANGER : new Color(fgColor.getRed(), fgColor.getGreen(), fgColor.getBlue(), 75));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font(FONT, Font.BOLD, 11));
        btn.setForeground(fgColor);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(64, 24));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> {
            listeningModule = module;
            listeningSlot   = slotName;
            listeningBtn    = btn;
            btn.setText("...");
            btn.setForeground(DANGER);
            btn.repaint();
        });
        return btn;
    }

    // --- SLOT KEY HELPERS ---
    private int getSlotKey(ClientModule module, String slot) {
        if (module instanceof TriggerBot && "slot1".equals(slot))
            return ((TriggerBot) module).getWeaponSlot();

        return switch (module) {
            case AnchorMacro m -> switch (slot) {
                case "slot1" -> m.getSlotAnchor();
                case "slot2" -> m.getSlotGlowstone();
                case "slot3" -> m.getSlotTotem();
                default -> -1;
            };
            case TNTCartMacro m -> switch (slot) {
                case "slot1" -> m.getSlotRail();
                case "slot2" -> m.getSlotCart();
                case "slot3" -> m.getSlotCrossbow();
                default -> -1;
            };
            case FireBowTNTCartMacro m -> switch (slot) {
                case "slot1" -> m.getSlotFireBow();
                case "slot2" -> m.getSlotRail();
                case "slot3" -> m.getSlotCart();
                default -> -1;
            };
            case FlintCrossbowTNTCartMacro m -> switch (slot) {
                case "slot1" -> m.getSlotRail();
                case "slot2" -> m.getSlotCart();
                case "slot3" -> m.getSlotFlint();
                case "slot4" -> m.getSlotCrossbow();
                default -> -1;
            };
            case MaceTech1 m -> switch (slot) {
                case "slot1" -> m.getSlotPearl();
                case "slot2" -> m.getSlotWindCharge();
                default -> -1;
            };
            default -> -1;
        };
    }

    private void setSlotKey(ClientModule module, String slot, int code) {
        if (module instanceof TriggerBot && "slot1".equals(slot)) {
            ((TriggerBot) module).setWeaponSlot(code);
            return;
        }
        switch (slot) {
            case "slot1" -> {
                switch (module) {
                    case AnchorMacro               m -> m.setSlotAnchor(code);
                    case TNTCartMacro              m -> m.setSlotRail(code);
                    case FireBowTNTCartMacro       m -> m.setSlotFireBow(code);
                    case FlintCrossbowTNTCartMacro m -> m.setSlotRail(code);
                    case MaceTech1                 m -> m.setSlotPearl(code);
                    default -> {}
                }
            }
            case "slot2" -> {
                switch (module) {
                    case AnchorMacro               m -> m.setSlotGlowstone(code);
                    case TNTCartMacro              m -> m.setSlotCart(code);
                    case FireBowTNTCartMacro       m -> m.setSlotRail(code);
                    case FlintCrossbowTNTCartMacro m -> m.setSlotCart(code);
                    case MaceTech1                 m -> m.setSlotWindCharge(code);
                    default -> {}
                }
            }
            case "slot3" -> {
                switch (module) {
                    case AnchorMacro               m -> m.setSlotTotem(code);
                    case TNTCartMacro              m -> m.setSlotCrossbow(code);
                    case FireBowTNTCartMacro       m -> m.setSlotCart(code);
                    case FlintCrossbowTNTCartMacro m -> m.setSlotFlint(code);
                    default -> {}
                }
            }
            case "slot4" -> {
                switch (module) {
                    case FlintCrossbowTNTCartMacro m -> m.setSlotCrossbow(code);
                    default -> {}
                }
            }
        }
    }

    // --- SHOW / HIDE ---
    private void showGUI() {
        guiOpenedAt = System.currentTimeMillis();
        SwingUtilities.invokeLater(() -> { setVisible(true); toFront(); requestFocus(); });
    }

    private void hideGUI() {
        SwingUtilities.invokeLater(() -> setVisible(false));
    }

    // --- NATIVE KEY LISTENER ---
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        int code = e.getKeyCode();

        // Listening for new GUI open keybind
        if (listeningGuiBind) {
            listeningGuiBind = false;
            guiKeybind = code;
            final String keyTxt = formatKey(NativeKeyEvent.getKeyText(code));
            SwingUtilities.invokeLater(() -> {
                guiBindBtn.setText(keyTxt);
                guiBindBtn.setForeground(ACCENT);
                guiBindBtn.repaint();
                SentaiHex.INSTANCE.configManager.save();
            });
            return;
        }

        // Listening for module/slot keybind
        if (listeningModule != null && listeningSlot != null && listeningBtn != null) {
            if (System.currentTimeMillis() - guiOpenedAt < 300) return;
            if (code == guiKeybind) return;

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
                btn.setForeground(mod.getCategory().equals("Function") ? FUNC_ACCENT : ACCENT);
                btn.repaint();
                SentaiHex.INSTANCE.configManager.save();
            });
            return;
        }

        if (code == guiKeybind) {
            if (isVisible()) hideGUI(); else showGUI();
        }
    }

    @Override public void nativeKeyReleased(NativeKeyEvent e) {}
    @Override public void nativeKeyTyped(NativeKeyEvent e) {}

    private String formatKey(String raw) {
        return raw.length() > 6 ? raw.substring(0, 5) + "." : raw;
    }
}