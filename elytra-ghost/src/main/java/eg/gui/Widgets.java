package eg.gui;

import eg.core.Setting;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

final class Widgets {
    private Widgets() {}

    static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    static GradientPaint accent(int x1, int x2) {
        return new GradientPaint(x1, 0, Theme.ACCENT_A, x2, 0, Theme.ACCENT_B);
    }

    /** Pill toggle. */
    static final class Switch extends Component {
        private final BooleanSupplier state;
        Switch(BooleanSupplier state, Runnable onClick) {
            this.state = state;
            setPreferredSize(new Dimension(40, 22));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { onClick.run(); repaint(); }
            });
        }
        @Override public void paint(Graphics g0) {
            Graphics2D g = (Graphics2D) g0; quality(g);
            int w = getWidth(), h = getHeight();
            boolean on = state.getAsBoolean();
            if (on) g.setPaint(accent(0, w)); else g.setColor(Theme.OFF);
            g.fillRoundRect(0, 0, w, h, h, h);
            g.setColor(Color.WHITE);
            int d = h - 6;
            g.fillOval(on ? w - d - 3 : 3, 3, d, d);
        }
    }

    /** Horizontal slider for a Num setting. */
    static final class Slider extends Component {
        private final Setting.Num s;
        Slider(Setting.Num s, Runnable onChange) {
            this.s = s;
            setPreferredSize(new Dimension(150, 22));
            MouseAdapter m = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { set(e.getX(), onChange); }
                @Override public void mouseDragged(MouseEvent e) { set(e.getX(), onChange); }
            };
            addMouseListener(m); addMouseMotionListener(m);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        private void set(int x, Runnable onChange) {
            double t = Math.max(0, Math.min(1, x / (double) Math.max(1, getWidth())));
            s.set(s.min + t * (s.max - s.min));
            onChange.run(); repaint();
        }
        @Override public void paint(Graphics g0) {
            Graphics2D g = (Graphics2D) g0; quality(g);
            int w = getWidth(), h = getHeight(), ty = h / 2 - 2;
            double t = (s.get() - s.min) / (s.max - s.min);
            g.setColor(Theme.OFF); g.fillRoundRect(0, ty, w, 4, 4, 4);
            int fw = (int) (w * t);
            g.setPaint(accent(0, w)); g.fillRoundRect(0, ty, Math.max(4, fw), 4, 4, 4);
            g.setColor(Color.WHITE); g.fillOval(Math.max(0, Math.min(w - 12, fw - 6)), h / 2 - 6, 12, 12);
        }
    }

    static final class Chip extends Component {
        private final Supplier text;
        interface Supplier { String get(); }
        Chip(Supplier text, Runnable onClick) {
            this.text = text;
            setPreferredSize(new Dimension(100, 22));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { onClick.run(); repaint(); }
            });
        }
        @Override public void paint(Graphics g0) {
            Graphics2D g = (Graphics2D) g0; quality(g);
            g.setColor(Theme.OFF); g.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            g.setColor(Theme.TEXT); g.setFont(Theme.SMALL);
            String t = text.get();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(t, (getWidth() - fm.stringWidth(t)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    static Component settingControl(Setting s, Runnable onChange) {
        if (s instanceof Setting.Bool b) return new Switch(b::get, () -> { b.set(!b.get()); onChange.run(); });
        if (s instanceof Setting.Num n) return new Slider(n, onChange);
        Setting.Mode m = (Setting.Mode) s;
        return new Chip(m::get, () -> { m.next(); onChange.run(); });
    }

    static String valueText(Setting s) {
        if (s instanceof Setting.Bool b) return b.get() ? "on" : "off";
        if (s instanceof Setting.Num n) return n.step >= 1 ? String.valueOf(n.getInt()) : String.format("%.2f", n.get());
        return ((Setting.Mode) s).get();
    }

    static void onClick(Component c, Consumer<MouseEvent> r) {
        c.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { r.accept(e); } });
    }
}
