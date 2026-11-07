package eg.core;

import eg.bridge.GameBridge;
import eg.bridge.UnresolvedBridge;
import eg.modules.AutoFirework;
import eg.modules.AutoTotem;
import eg.modules.ElytraSwap;
import eg.modules.TargetAim;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class GhostClient {
    private static final GhostClient INSTANCE = new GhostClient();
    public static GhostClient get() { return INSTANCE; }

    private final List<Module> modules = List.of(new TargetAim(), new AutoTotem(), new AutoFirework(), new ElytraSwap());
    private volatile GameBridge bridge = new UnresolvedBridge();
    private volatile Instrumentation instrumentation;
    private ScheduledExecutorService ticker;
    private ScheduledFuture<?> tickTask;
    private Runnable onPanic = () -> {};

    private GhostClient() {}

    public List<Module> modules() { return modules; }
    public GameBridge bridge() { return bridge; }
    public void setBridge(GameBridge b) { bridge = b == null ? new UnresolvedBridge() : b; }
    public Instrumentation instrumentation() { return instrumentation; }
    public void onPanic(Runnable r) { onPanic = r; }

    public synchronized void start(Instrumentation inst) {
        instrumentation = inst;
        if (tickTask != null) return;
        ticker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "eg-ticker");
            t.setDaemon(true);
            return t;
        });
        tickTask = ticker.scheduleAtFixedRate(this::tick, 50, 50, TimeUnit.MILLISECONDS);
    }

    /** One tick; package-visible so tests can drive it without a thread. */
    public void tick() {
        GameBridge b = bridge;
        boolean ready;
        try { ready = b.ready(); } catch (Throwable t) { ready = false; }
        if (!ready) return;
        for (Module m : modules) {
            if (!m.isEnabled()) continue;
            try { m.onTick(b); }
            catch (Throwable t) {
                log("module " + m.name + " crashed, disabled: " + t);
                m.setEnabled(false);
            }
        }
    }

    /** Disable everything and hide the GUI. */
    public void panic() {
        for (Module m : modules) m.setEnabled(false);
        try { onPanic.run(); } catch (Throwable ignored) {}
    }

    /**
     * Stops ticking and disables modules. Agent classes stay loaded in the target JVM;
     * an attached agent cannot be unloaded.
     */
    public synchronized void shutdown() {
        panic();
        if (tickTask != null) { tickTask.cancel(false); tickTask = null; }
        if (ticker != null) { ticker.shutdownNow(); ticker = null; }
    }

    public static void log(String s) { System.err.println("[elytra-ghost] " + s); }
}
