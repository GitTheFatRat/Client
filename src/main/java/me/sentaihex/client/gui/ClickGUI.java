package me.sentaihex.client.gui;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import me.sentaihex.client.SentaiHex;
import me.sentaihex.client.module.ClientModule;

import me.sentaihex.client.module.macros.*;
import me.sentaihex.client.module.function.XPBottleSpammer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;

public class ClickGUI extends JFrame implements NativeKeyListener {

    // === GLASS THEME ===
    private static final Color GLASS_BG     = new Color(10, 10, 18, 210);
    private static final Color GLASS_CARD   = new Color(255, 255, 255, 18);
    private static final Color GLASS_BORDER = new Color(255, 255, 255, 40);
    private static final Color GLASS_HOVER  = new Color(255, 255, 255, 30);
    private static final Color ACCENT       = new Color(130, 180, 255);
    private static final Color TEXT_MAIN    = new Color(235, 240, 255);
    private static final Color TEXT_MUTED   = new Color(140, 155, 185);
    private static final Color TOGGLE_ON    = new Color(100, 200, 120);
    private static final Color TOGGLE_OFF   = new Color(60, 65, 85);
    private static final Color DANGER       = new Color(255, 80, 100);

    private static final Color FUNC_CARD   = new Color(255, 140, 60, 18);
    private static final Color FUNC_BORDER = new Color(255, 140, 60, 60);
    private static final Color FUNC_HOVER  = new Color(255, 140, 60, 30);
    private static final Color FUNC_ACCENT = new Color(255, 160, 80);

    private static final int    RADIUS = 20;
    private static final String FONT   = "Segoe UI"; // NOSONAR - font name spelling

    private ClientModule listeningModule = null;
    private String       listeningSlot   = null;
    private JButton      listeningBtn    = null;
    private Point        dragPoint       = null;
    private volatile long guiOpenedAt   = 0;

    public ClickGUI() {
        setTitle("SentaiHex");
        setSize(460, 620);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setAlwaysOnTop(true);

        if (getContentPane() instanceof JComponent jc) jc.setOpaque(false);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), RADIUS * 2, RADIUS * 2));
            }
        });

        setContentPane(buildRoot());
        GlobalScreen.addNativeKeyListener(this);
    }

    // ─── ROOT ──────────────────────────────────────────────────────────────────
    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GLASS_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS * 2, RADIUS * 2);
                g2.setColor(new Color(255, 255, 255, 25));
                g2.fillRoundRect(1, 1, getWidth() - 2, 60, RADIUS * 2, RADIUS * 2);
                g2.setColor(GLASS_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS * 2, RADIUS * 2);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildScrollContent(), BorderLayout.CENTER);
        return root;
    }

    // ─── HEADER ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = createHeaderPanel();
        header.setPreferredSize(new Dimension(460, 58));
        header.setBorder(new EmptyBorder(0, 20, 0, 12));
        header.add(buildHeaderLeft(), BorderLayout.WEST);
        header.add(buildCloseButton(), BorderLayout.EAST);
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

    private JPanel createHeaderPanel() {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 12));
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + RADIUS, RADIUS * 2, RADIUS * 2);
                g2.setColor(new Color(255, 255, 255, 20));
                g2.drawLine(16, getHeight() - 1, getWidth() - 16, getHeight() - 1);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    private JPanel buildHeaderLeft() {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel icon = new JLabel("+");
        icon.setFont(new Font(FONT, Font.BOLD, 20));
        icon.setForeground(ACCENT);
        JLabel title = new JLabel("SentaiHex");
        title.setFont(new Font(FONT, Font.BOLD, 16));
        title.setForeground(TEXT_MAIN);
        JLabel ver = new JLabel("v1.0");
        ver.setFont(new Font(FONT, Font.PLAIN, 11));
        ver.setForeground(TEXT_MUTED);
        left.add(icon); left.add(title); left.add(ver);
        return left;
    }

    private JPanel buildCloseButton() {
        JButton closeBtn = new JButton("X");
        closeBtn.setFont(new Font(FONT, Font.PLAIN, 14));
        closeBtn.setForeground(TEXT_MUTED);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { closeBtn.setForeground(DANGER); }
            @Override public void mouseExited(MouseEvent e)  { closeBtn.setForeground(TEXT_MUTED); }
        });
        closeBtn.addActionListener(e -> hideGUI());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(closeBtn);
        return right;
    }

    // ─── SCROLL CONTENT ────────────────────────────────────────────────────────
    private JScrollPane buildScrollContent() {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new GridBagLayout());
        listPanel.setOpaque(false);
        listPanel.setBorder(new EmptyBorder(12, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);

        int row = 0;

        java.util.List<ClientModule> macros = SentaiHex.INSTANCE.moduleManager.getByCategory("Macro");
        if (!macros.isEmpty()) {
            listPanel.add(buildSectionLabel("  MACROS", TEXT_MUTED), gbc(gbc, row++, 0));
            for (int i = 0; i < macros.size(); i++)
                listPanel.add(buildMacroCard(macros.get(i)), gbc(gbc, row++, i < macros.size() - 1 ? 10 : 0));
        }

        java.util.List<ClientModule> functions = SentaiHex.INSTANCE.moduleManager.getByCategory("Function");
        if (!functions.isEmpty()) {
            listPanel.add(buildSectionLabel("  FUNCTIONS", new Color(255, 160, 80)), gbc(gbc, row++, 0));
            for (int i = 0; i < functions.size(); i++)
                listPanel.add(buildFunctionCard(functions.get(i)), gbc(gbc, row++, i < functions.size() - 1 ? 10 : 0));
        }

        GridBagConstraints glue = new GridBagConstraints();
        glue.gridx = 0; glue.gridy = row;
        glue.weighty = 1.0; glue.fill = GridBagConstraints.VERTICAL;
        listPanel.add(Box.createVerticalGlue(), glue);

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(4, 0));
        return scroll;
    }

    private GridBagConstraints gbc(GridBagConstraints base, int row, int bottomInset) {
        base.gridy = row;
        base.insets = new Insets(0, 0, bottomInset, 0);
        return base;
    }

    private JLabel buildSectionLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(FONT, Font.BOLD, 10));
        label.setForeground(color);
        label.setBorder(new EmptyBorder(0, 4, 8, 0));
        return label;
    }

    // ─── CARD SHARED ──────────────────────────────────────────────────────────
    private JPanel buildCard(boolean isMacro) {
        Color normal = isMacro ? GLASS_CARD : FUNC_CARD;
        Color hover  = isMacro ? GLASS_HOVER : FUNC_HOVER;
        Color border = isMacro ? GLASS_BORDER : FUNC_BORDER;
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
                g2.setColor(hovered ? hover : normal);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS, RADIUS);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS, RADIUS);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 0));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 999));
        card.setMinimumSize(new Dimension(100, 50));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    // ─── MACRO CARD ────────────────────────────────────────────────────────────
    private JPanel buildMacroCard(ClientModule m) {
        JPanel card = buildCard(true);
        String displayName = switch (m.getName()) {
            case "Anchor Bomb"                    -> "Respawn Anchor";
            case "TNT Cart"                       -> "TNT Cart";
            case "TNT Cart (Fire Bow)"            -> "TNT Cart (Fire Bow)";
            case "TNT Cart (Flint + Crossbow)"    -> "TNT Cart (Flint + Crossbow)";
            case "Mace Tech 1 (Pearl+Wind)"       -> "Pearl + Wind Charge";
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

    // ─── FUNCTION CARD ────────────────────────────────────────────────────────
    private JPanel buildFunctionCard(ClientModule m) {
        JPanel card = buildCard(false);
        card.add(buildTopRow(m, m.getName(), false), BorderLayout.NORTH);
        if (!isToggleOnly(m)) {
            JPanel body = buildFunctionBody(m);
            card.add(body, BorderLayout.CENTER);
        }
        return card;
    }

    private JPanel buildFunctionBody(ClientModule m) {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(10, 0, 0, 0));
        body.add(buildDelayRow(m, true));
        body.add(Box.createVerticalStrut(6));
        body.add(buildBindRow(m, true));
        return body;
    }

    private boolean isToggleOnly(ClientModule m) {
        return m instanceof XPBottleSpammer;
    }

    // ─── TOP ROW ──────────────────────────────────────────────────────────────
    private JPanel buildTopRow(ClientModule m, String displayName, boolean isMacro) {
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(new Font(FONT, Font.BOLD, 14));
        nameLabel.setForeground(TEXT_MAIN);
        topRow.add(nameLabel, BorderLayout.WEST);
        topRow.add(buildToggle(m, isMacro), BorderLayout.EAST);
        return topRow;
    }

    // ─── TOGGLE ────────────────────────────────────────────────────────────────
    private JComponent buildToggle(ClientModule m, boolean isMacro) {
        JButton toggle = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(m.isEnabled() ? (isMacro ? TOGGLE_ON : FUNC_ACCENT) : TOGGLE_OFF);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(new Color(255, 255, 255, 220));
                int knob = getHeight() - 4;
                int x = m.isEnabled() ? getWidth() - knob - 2 : 2;
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

    // ─── SLOTS ─────────────────────────────────────────────────────────────────
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

    // ─── SLOT ROW ──────────────────────────────────────────────────────────────
    private JPanel buildSlotRow(ClientModule module, String slotName, String label) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(FONT, Font.PLAIN, 12));
        lbl.setForeground(TEXT_MUTED);
        row.add(lbl, BorderLayout.WEST);
        row.add(buildBindBtn(module, slotName, false), BorderLayout.EAST);
        return row;
    }

    // ─── DELAY ROW ─────────────────────────────────────────────────────────────
    private JPanel buildDelayRow(ClientModule m, boolean isFn) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel("Delay");
        lbl.setFont(new Font(FONT, Font.PLAIN, 12));
        lbl.setForeground(TEXT_MUTED);
        row.add(lbl, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);

        JTextField field = isFn ? buildDelayFieldFn(m) : buildDelayFieldMacro(m);
        Color accentColor = isFn ? FUNC_ACCENT : ACCENT;
        Runnable apply = buildDelayApply(m, field, accentColor);
        field.addActionListener(e -> apply.run());
        field.addFocusListener(new FocusAdapter() { @Override public void focusLost(FocusEvent e) { apply.run(); } });

        JLabel ms = new JLabel("ms");
        ms.setFont(new Font(FONT, Font.PLAIN, 11));
        ms.setForeground(TEXT_MUTED);
        right.add(field); right.add(ms);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    private JTextField buildDelayFieldFn(ClientModule m) {
        JTextField field = new JTextField(String.valueOf(m.getGlobalDelay()), 4);
        field.setFont(new Font(FONT, Font.BOLD, 12));
        field.setForeground(FUNC_ACCENT);
        field.setBackground(new Color(30, 15, 5));
        field.setBorder(BorderFactory.createLineBorder(FUNC_BORDER));
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setPreferredSize(new Dimension(52, 24));
        return field;
    }

    private JTextField buildDelayFieldMacro(ClientModule m) {
        JTextField field = new JTextField(String.valueOf(m.getGlobalDelay()), 4) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 15));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(GLASS_BORDER);
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

    private JPanel buildAimRow(String labelText, int initialMs, java.util.function.IntConsumer onApply) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font(FONT, Font.PLAIN, 12));
        lbl.setForeground(TEXT_MUTED);
        row.add(lbl, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);

        JTextField field = new JTextField(String.valueOf(initialMs), 4) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 15));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(GLASS_BORDER);
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

        JLabel ms = new JLabel("ms");
        ms.setFont(new Font(FONT, Font.PLAIN, 11));
        ms.setForeground(TEXT_MUTED);
        right.add(field);
        right.add(ms);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    // ─── BIND ROW ─────────────────────────────────────────────────────────────
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

    // ─── BIND BUTTON ──────────────────────────────────────────────────────────
    private JButton buildBindBtn(ClientModule module, String slotName, boolean isFn) {
        int cur = slotName.equals("mainBind") ? module.getKeybind() : getSlotKey(module, slotName);
        String txt = cur == -1 ? "—" : formatKey(NativeKeyEvent.getKeyText(cur));
        Color fgColor = isFn ? FUNC_ACCENT : ACCENT;
        JButton btn = new JButton(txt) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean listening = this == listeningBtn;
                g2.setColor(listening ? new Color(255, 80, 100, 40) : new Color(255, 255, 255, 12));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(listening ? DANGER : (isFn ? FUNC_BORDER : GLASS_BORDER));
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

    // ─── SLOT KEY HELPERS ─────────────────────────────────────────────────────
    private int getSlotKey(ClientModule module, String slot) {
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
        switch (slot) {
            case "slot1" -> {
                switch (module) {
                    case AnchorMacro                m -> m.setSlotAnchor(code);
                    case TNTCartMacro               m -> m.setSlotRail(code);
                    case FireBowTNTCartMacro        m -> m.setSlotFireBow(code);
                    case FlintCrossbowTNTCartMacro  m -> m.setSlotRail(code);
                    case MaceTech1                  m -> m.setSlotPearl(code);
                    default -> {}
                }
            }
            case "slot2" -> {
                switch (module) {
                    case AnchorMacro                m -> m.setSlotGlowstone(code);
                    case TNTCartMacro               m -> m.setSlotCart(code);
                    case FireBowTNTCartMacro        m -> m.setSlotRail(code);
                    case FlintCrossbowTNTCartMacro  m -> m.setSlotCart(code);
                    case MaceTech1                  m -> m.setSlotWindCharge(code);
                    default -> {}
                }
            }
            case "slot3" -> {
                switch (module) {
                    case AnchorMacro                m -> m.setSlotTotem(code);
                    case TNTCartMacro               m -> m.setSlotCrossbow(code);
                    case FireBowTNTCartMacro        m -> m.setSlotCart(code);
                    case FlintCrossbowTNTCartMacro  m -> m.setSlotFlint(code);
                    default -> {}
                }
            }
            case "slot4" -> {
                switch (module) {
                    case FlintCrossbowTNTCartMacro  m -> m.setSlotCrossbow(code);
                    default -> {}
                }
            }
        }
    }

    // ─── SHOW / HIDE ──────────────────────────────────────────────────────────
    private void showGUI() {
        guiOpenedAt = System.currentTimeMillis();
        SwingUtilities.invokeLater(() -> { setVisible(true); toFront(); requestFocus(); });
    }

    private void hideGUI() {
        SwingUtilities.invokeLater(() -> setVisible(false));
    }

    // ─── NATIVE KEY ───────────────────────────────────────────────────────────
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
            if (isVisible()) hideGUI(); else showGUI();
        }
    }

    @Override public void nativeKeyReleased(NativeKeyEvent e) {}
    @Override public void nativeKeyTyped(NativeKeyEvent e) {}
    private String formatKey(String raw) {
        return raw.length() > 6 ? raw.substring(0, 5) + "." : raw;
    }
}