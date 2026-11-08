package eg.gui;

import eg.core.GhostClient;
import eg.core.Module;
import eg.core.Setting;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Always-on-top dark window with category tabs, module cards and live settings. */
public final class ClickGui extends JFrame {
    private static ClickGui instance;

    public static void open() {
        SwingUtilities.invokeLater(() -> {
            if (instance == null) {
                instance = new ClickGui();
                GhostClient.get().onPanic(() -> SwingUtilities.invokeLater(() -> { if (instance != null) instance.setVisible(false); }));
            }
            instance.setVisible(true);
            instance.setExtendedState(JFrame.NORMAL);
            instance.toFront();
            instance.requestFocus();
        });
    }

    private final JPanel list = new JPanel();
    private final JLabel status = new JLabel();
    private Module.Category current = Module.Category.COMBAT;
    private final javax.swing.Timer statusTimer;

    private ClickGui() {
        super("Elytra Ghost");
        // Deliberately a normal, decorated, fully opaque window: an undecorated window with
        // a per-pixel-transparent background depends on the desktop compositor supporting
        // translucency for THIS window's GraphicsConfiguration. When it doesn't (seen in
        // practice running inside the same process as a game that grabbed an exclusive
        // OpenGL/DirectX surface), Swing doesn't fall back to an opaque window - the whole
        // window just never becomes visible, with no exception thrown anywhere. A normal
        // decorated JFrame has no such dependency: it always renders and always gets a
        // taskbar entry.
        setAlwaysOnTop(true);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setSize(560, 520);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0; Widgets.quality(g);
                g.setColor(Theme.BG); g.fillRect(0, 0, getWidth(), getHeight());
                g.setPaint(Widgets.accent(0, getWidth())); g.fillRect(0, 0, getWidth(), 3);
            }
        };
        root.setOpaque(true);
        setContentPane(root);

        root.add(header(), BorderLayout.NORTH);
        root.add(sidebar(), BorderLayout.WEST);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        list.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 12));
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null); sp.setOpaque(false); sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        root.add(sp, BorderLayout.CENTER);

        status.setFont(Theme.SMALL); status.setForeground(Theme.MUTED);
        status.setBorder(BorderFactory.createEmptyBorder(6, 14, 8, 14));
        root.add(status, BorderLayout.SOUTH);

        rebuild();
        statusTimer = new javax.swing.Timer(500, e -> {
            boolean ready; String d;
            try { ready = GhostClient.get().bridge().ready(); d = GhostClient.get().bridge().describe(); }
            catch (Throwable t) { ready = false; d = "bridge error: " + t; }
            status.setForeground(ready ? Theme.ACCENT_B : Theme.DANGER);
            status.setText((ready ? "Bridge ready - " : "Bridge NOT ready - ") + d); status.setToolTipText(d);
        });
        statusTimer.start();
    }

    private JComponent header() {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        h.setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 14));
        JLabel title = new JLabel("ELYTRA GHOST");
        title.setFont(Theme.TITLE); title.setForeground(Theme.TEXT);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(button("PANIC", Theme.DANGER, () -> GhostClient.get().panic()));
        right.add(button("HIDE", Theme.OFF, () -> setVisible(false)));
        h.add(title, BorderLayout.WEST); h.add(right, BorderLayout.EAST);
        MouseAdapter drag = new MouseAdapter() {
            Point start;
            @Override public void mousePressed(MouseEvent e) { start = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                Point p = getLocation(); setLocation(p.x + e.getX() - start.x, p.y + e.getY() - start.y);
            }
        };
        title.addMouseListener(drag); title.addMouseMotionListener(drag);
        h.addMouseListener(drag); h.addMouseMotionListener(drag);
        return h;
    }

    private static JComponent button(String text, Color c, Runnable r) {
        JLabel b = new JLabel(text, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0; Widgets.quality(g);
                g.setColor(c); g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
            }
        };
        b.setOpaque(false); b.setForeground(Color.WHITE); b.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        b.setPreferredSize(new Dimension(64, 26));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Widgets.onClick(b, e -> r.run());
        return b;
    }

    private JComponent sidebar() {
        JPanel s = new JPanel();
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
        s.setOpaque(false);
        s.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 0));
        for (Module.Category c : Module.Category.values()) {
            JLabel t = new JLabel(c.name()) {
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0; Widgets.quality(g);
                    if (current == c) { g.setPaint(Widgets.accent(0, getWidth())); g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); }
                    else { g.setColor(Theme.PANEL); g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); }
                    super.paintComponent(g);
                }
            };
            t.setOpaque(false); t.setFont(Theme.BOLD);
            t.setForeground(Theme.TEXT);
            t.setHorizontalAlignment(SwingConstants.CENTER);
            t.setMaximumSize(new Dimension(110, 34)); t.setPreferredSize(new Dimension(110, 34));
            t.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Widgets.onClick(t, e -> { current = c; rebuild(); s.repaint(); });
            s.add(t); s.add(Box.createVerticalStrut(6));
        }
        return s;
    }

    private void rebuild() {
        list.removeAll();
        for (Module m : GhostClient.get().modules())
            if (m.category == current) { list.add(card(m)); list.add(Box.createVerticalStrut(8)); }
        list.revalidate(); list.repaint();
    }

    private JComponent card(Module m) {
        JPanel card = new JPanel() {
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0; Widgets.quality(g);
                g.setColor(Theme.CARD); g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                if (m.isEnabled()) { g.setPaint(Widgets.accent(0, getWidth())); g.fillRoundRect(0, 10, 3, 34, 3, 3); }
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel names = new JPanel(); names.setOpaque(false); names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        JLabel n = new JLabel(m.name); n.setFont(Theme.BOLD); n.setForeground(Theme.TEXT);
        JLabel d = new JLabel(m.description); d.setFont(Theme.SMALL); d.setForeground(Theme.MUTED);
        names.add(n); names.add(d);
        top.add(names, BorderLayout.CENTER);
        top.add(new Widgets.Switch(m::isEnabled, () -> { m.toggle(); card.repaint(); }), BorderLayout.EAST);
        card.add(top);

        JPanel body = new JPanel(); body.setOpaque(false); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setVisible(false);
        for (Setting s : m.settings()) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false); row.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            JLabel l = new JLabel(s.name); l.setFont(Theme.SMALL); l.setForeground(Theme.MUTED);
            l.setPreferredSize(new Dimension(130, 20));
            JLabel v = new JLabel(Widgets.valueText(s)); v.setFont(Theme.SMALL); v.setForeground(Theme.TEXT);
            v.setPreferredSize(new Dimension(48, 20));
            Component c = Widgets.settingControl(s, () -> v.setText(Widgets.valueText(s)));
            row.add(l, BorderLayout.WEST); row.add(c, BorderLayout.CENTER); row.add(v, BorderLayout.EAST);
            body.add(row);
        }
        card.add(body);
        Widgets.onClick(names, e -> { body.setVisible(!body.isVisible()); card.revalidate(); list.revalidate(); list.repaint(); });
        names.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        n.setToolTipText("Click to show settings");
        Widgets.onClick(n, e -> { body.setVisible(!body.isVisible()); card.revalidate(); list.revalidate(); list.repaint(); });
        return card;
    }
}
