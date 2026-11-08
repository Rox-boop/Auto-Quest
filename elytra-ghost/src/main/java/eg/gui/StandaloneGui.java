package eg.gui;

import eg.core.ControlServer;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The actual menu, as its own process (see eg.core.GhostClient.toggleMenu() for why: the
 * game process is typically launched with -Djava.awt.headless=true, which permanently
 * blocks any AWT/Swing window inside it). Talks to the running game only through
 * ControlServer's small localhost socket protocol - never touches any game class directly,
 * so it needs no per-build bridge and works the same for vanilla and Fabric.
 */
public final class StandaloneGui extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StandaloneGui().setVisible(true));
    }

    private record RemoteSetting(String module, String name, String kind, String value, String extra) {}
    private record RemoteModule(String name, String description, String category, boolean enabled, List<RemoteSetting> settings) {}

    private final JPanel list = new JPanel();
    private final JLabel status = new JLabel("Connecting...");
    private String currentCategory = null;
    private List<RemoteModule> modules = List.of();

    private StandaloneGui() {
        super("Elytra Ghost");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(560, 520);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG);
        header.setBorder(BorderFactory.createEmptyBorder(14, 18, 10, 14));
        JLabel title = new JLabel("ELYTRA GHOST");
        title.setFont(Theme.TITLE);
        title.setForeground(Theme.TEXT);
        header.add(title, BorderLayout.WEST);
        JButton panicButton = flatButton("PANIC", Theme.DANGER);
        panicButton.addActionListener(e -> send("PANIC"));
        header.add(panicButton, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Theme.BG);
        list.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        JScrollPane sp = new JScrollPane(list);
        sp.getViewport().setBackground(Theme.BG);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        root.add(sp, BorderLayout.CENTER);

        status.setFont(Theme.SMALL);
        status.setForeground(Theme.MUTED);
        status.setBorder(BorderFactory.createEmptyBorder(6, 14, 8, 14));
        root.add(status, BorderLayout.SOUTH);

        new javax.swing.Timer(500, e -> refresh()).start();
        refresh();
    }

    private static JButton flatButton(String text, Color c) {
        JButton b = new JButton(text);
        b.setBackground(c);
        b.setForeground(Color.WHITE);
        b.setFont(Theme.SMALL.deriveFont(Font.BOLD));
        b.setFocusPainted(false);
        return b;
    }

    // --- networking ---

    private void refresh() {
        new Thread(() -> {
            List<RemoteModule> newModules = null;
            String newStatus;
            try {
                newModules = fetchList();
                newStatus = fetchStatus();
            } catch (IOException e) {
                newStatus = "Not connected to the game (" + e.getMessage() + "). Is Minecraft still running with the agent injected?";
            }
            List<RemoteModule> finalModules = newModules;
            String finalStatus = newStatus;
            SwingUtilities.invokeLater(() -> {
                status.setText(finalStatus);
                if (finalModules != null) { modules = finalModules; rebuild(); }
            });
        }, "eg-gui-refresh").start();
    }

    private List<RemoteModule> fetchList() throws IOException {
        List<String> lines = roundTrip("LIST");
        Map<String, List<RemoteSetting>> settingsByModule = new LinkedHashMap<>();
        Map<String, String[]> moduleLines = new LinkedHashMap<>();
        List<String> order = new ArrayList<>();
        for (String l : lines) {
            if (l.startsWith("MODULE ")) {
                String[] f = l.substring(7).split("\\|", -1);
                moduleLines.put(f[0], f);
                order.add(f[0]);
                settingsByModule.put(f[0], new ArrayList<>());
            } else if (l.startsWith("SETTING ")) {
                String[] f = l.substring(8).split("\\|", -1);
                settingsByModule.computeIfAbsent(f[0], k -> new ArrayList<>())
                        .add(new RemoteSetting(f[0], f[1], f[2], f[3], f.length > 4 ? f[4] : ""));
            }
        }
        List<RemoteModule> result = new ArrayList<>();
        for (String name : order) {
            String[] f = moduleLines.get(name);
            result.add(new RemoteModule(f[0], f[1], f[2], Boolean.parseBoolean(f[3]), settingsByModule.get(name)));
        }
        return result;
    }

    private String fetchStatus() throws IOException {
        List<String> lines = roundTrip("STATUS");
        if (lines.size() < 2) return "unexpected STATUS reply";
        return (lines.get(0).startsWith("READY true") ? "Bridge ready - " : "Bridge NOT ready - ") + lines.get(1);
    }

    private void send(String cmd) {
        new Thread(() -> {
            try { roundTrip(cmd); } catch (IOException e) { /* surfaced on next refresh's status line */ }
            refresh();
        }, "eg-gui-send").start();
    }

    private static List<String> roundTrip(String cmd) throws IOException {
        try (Socket s = new Socket(InetAddress.getLoopbackAddress(), ControlServer.PORT)) {
            s.setSoTimeout(2000);
            PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
            out.println(cmd);
            List<String> lines = new ArrayList<>();
            boolean multiLine = cmd.equals("LIST") || cmd.equals("STATUS");
            String line;
            while ((line = in.readLine()) != null) {
                if (multiLine) {
                    if (line.equals(".")) break;
                    lines.add(line);
                } else {
                    lines.add(line);
                    break;
                }
            }
            return lines;
        }
    }

    // --- UI build ---

    private void rebuild() {
        if (currentCategory == null && !modules.isEmpty()) currentCategory = modules.get(0).category();
        list.removeAll();

        java.util.LinkedHashSet<String> categories = new java.util.LinkedHashSet<>();
        for (RemoteModule m : modules) categories.add(m.category());

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tabs.setBackground(Theme.BG);
        for (String cat : categories) {
            JButton b = flatButton(cat, cat.equals(currentCategory) ? Theme.ACCENT_A : Theme.OFF);
            b.addActionListener(e -> { currentCategory = cat; rebuild(); });
            tabs.add(b);
        }
        list.add(tabs);
        list.add(Box.createVerticalStrut(6));

        for (RemoteModule m : modules) {
            if (!m.category().equals(currentCategory)) continue;
            list.add(card(m));
            list.add(Box.createVerticalStrut(8));
        }
        list.revalidate();
        list.repaint();
    }

    private JComponent card(RemoteModule m) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.CARD);
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel names = new JPanel();
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.setOpaque(false);
        JLabel n = new JLabel(m.name());
        n.setFont(Theme.BOLD);
        n.setForeground(Theme.TEXT);
        JLabel d = new JLabel(m.description());
        d.setFont(Theme.SMALL);
        d.setForeground(Theme.MUTED);
        names.add(n);
        names.add(d);
        top.add(names, BorderLayout.CENTER);

        JToggleButton toggle = new JToggleButton(m.enabled() ? "ON" : "OFF", m.enabled());
        toggle.setBackground(m.enabled() ? Theme.ACCENT_B : Theme.OFF);
        toggle.setForeground(Color.WHITE);
        toggle.setFocusPainted(false);
        toggle.addActionListener(e -> send("TOGGLE " + m.name()));
        top.add(toggle, BorderLayout.EAST);
        card.add(top);

        for (RemoteSetting s : m.settings()) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            JLabel l = new JLabel(s.name());
            l.setFont(Theme.SMALL);
            l.setForeground(Theme.MUTED);
            l.setPreferredSize(new Dimension(130, 20));
            row.add(l, BorderLayout.WEST);
            row.add(settingControl(s), BorderLayout.CENTER);
            card.add(row);
        }
        return card;
    }

    private JComponent settingControl(RemoteSetting s) {
        switch (s.kind()) {
            case "BOOL" -> {
                boolean v = Boolean.parseBoolean(s.value());
                JToggleButton b = new JToggleButton(v ? "on" : "off", v);
                b.addActionListener(e -> send("SETBOOL " + s.module() + " " + s.name() + " " + b.isSelected()));
                return b;
            }
            case "NUM" -> {
                String[] mm = s.extra().split(",");
                double min = Double.parseDouble(mm[0]), max = Double.parseDouble(mm[1]), step = Double.parseDouble(mm[2]);
                double value = Double.parseDouble(s.value());
                int scale = 1000;
                JSlider slider = new JSlider((int) (min * scale), (int) (max * scale), (int) (value * scale));
                JLabel v = new JLabel(s.value());
                v.setForeground(Theme.TEXT);
                v.setFont(Theme.SMALL);
                slider.addChangeListener(e -> {
                    double newVal = slider.getValue() / (double) scale;
                    v.setText(String.format("%.2f", newVal));
                    if (!slider.getValueIsAdjusting()) send("SETNUM " + s.module() + " " + s.name() + " " + newVal);
                });
                JPanel p = new JPanel(new BorderLayout(6, 0));
                p.setOpaque(false);
                p.add(slider, BorderLayout.CENTER);
                p.add(v, BorderLayout.EAST);
                return p;
            }
            default -> {
                JButton b = new JButton(s.value());
                b.addActionListener(e -> send("NEXTMODE " + s.module() + " " + s.name()));
                return b;
            }
        }
    }
}
