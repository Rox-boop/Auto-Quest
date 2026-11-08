package eg.inject;

import com.sun.tools.attach.VirtualMachineDescriptor;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A small double-click-to-open front end for {@link Injector}: lists attachable JVMs,
 * highlights the ones that look like a Minecraft launcher/client, and injects into the
 * selected one on a button press. Pure convenience over the CLI path - same attach call,
 * same jar, no extra capability.
 */
final class InjectorGui extends JFrame {
    private final DefaultListModel<VirtualMachineDescriptor> model = new DefaultListModel<>();
    private final JList<VirtualMachineDescriptor> list = new JList<>(model);
    private final JLabel status = new JLabel("Scanning for running Java processes...");
    private final JButton injectButton = new JButton("Inject");
    private final ScheduledExecutorService refresher = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "eg-injector-gui-refresh");
        t.setDaemon(true);
        return t;
    });

    static void open() {
        SwingUtilities.invokeLater(() -> new InjectorGui().setVisible(true));
    }

    private InjectorGui() {
        super("Elytra Ghost - Injector");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(460, 360);
        setLocationRelativeTo(null);

        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object value, int index,
                    boolean isSelected, boolean hasFocus) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, value, index, isSelected, hasFocus);
                VirtualMachineDescriptor d = (VirtualMachineDescriptor) value;
                boolean likely = Injector.looksLikeMinecraft(d.displayName());
                c.setText("<html>" + (likely ? "<b>" : "") + "PID " + d.id() + " - " + escape(d.displayName())
                        + (likely ? " &nbsp;&larr; likely Minecraft</b>" : "") + "</html>");
                return c;
            }
        });
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        status.setBorder(BorderFactory.createEmptyBorder(6, 10, 0, 10));
        bottom.add(status, BorderLayout.NORTH);
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(refreshButton);
        buttons.add(injectButton);
        bottom.add(buttons, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        injectButton.setEnabled(false);
        injectButton.addActionListener(e -> doInject());
        list.addListSelectionListener(e -> injectButton.setEnabled(list.getSelectedValue() != null));

        refresh();
        refresher.scheduleAtFixedRate(this::refresh, 3, 3, TimeUnit.SECONDS);
    }

    private void refresh() {
        VirtualMachineDescriptor selected = list.getSelectedValue();
        List<VirtualMachineDescriptor> vms;
        try {
            vms = Injector.listVms();
        } catch (Throwable t) {
            SwingUtilities.invokeLater(() -> status.setText("Failed to list Java processes: " + t));
            return;
        }
        SwingUtilities.invokeLater(() -> {
            model.clear();
            // likely-Minecraft entries first
            vms.stream().filter(d -> Injector.looksLikeMinecraft(d.displayName())).forEach(model::addElement);
            vms.stream().filter(d -> !Injector.looksLikeMinecraft(d.displayName())).forEach(model::addElement);
            long likely = vms.stream().filter(d -> Injector.looksLikeMinecraft(d.displayName())).count();
            status.setText(likely > 0
                    ? "Found " + likely + " likely Minecraft process(es). Select one and click Inject."
                    : "No obvious Minecraft process found. Start Minecraft, or pick one manually below.");
            if (selected != null)
                for (int i = 0; i < model.size(); i++)
                    if (model.get(i).id().equals(selected.id())) { list.setSelectedIndex(i); break; }
        });
    }

    private void doInject() {
        VirtualMachineDescriptor d = list.getSelectedValue();
        if (d == null) return;
        injectButton.setEnabled(false);
        injectButton.setText("Injecting...");
        new Thread(() -> {
            String result;
            try {
                Injector.inject(d.id());
                result = "Injected into PID " + d.id() + ". Check the game's own window/console for "
                        + "[elytra-ghost] log lines and the Elytra Ghost window.";
            } catch (Throwable t) {
                result = "Injection failed: " + t;
            }
            String finalResult = result;
            SwingUtilities.invokeLater(() -> {
                status.setText(finalResult);
                injectButton.setText("Inject");
                injectButton.setEnabled(true);
            });
        }, "eg-inject-click").start();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
