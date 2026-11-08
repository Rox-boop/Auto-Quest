package eg.core;

import eg.bridge.GameBridge;
import eg.bridge.UnresolvedBridge;
import eg.modules.AutoFirework;
import eg.modules.AutoTotem;
import eg.modules.ElytraSwap;
import eg.modules.TargetAim;

import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URISyntaxException;
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
    private final ControlServer controlServer = new ControlServer();
    private boolean menuKeyWasDown;
    private Process menuProcess;

    private GhostClient() {}

    public List<Module> modules() { return modules; }
    public GameBridge bridge() { return bridge; }
    public void setBridge(GameBridge b) { bridge = b == null ? new UnresolvedBridge() : b; }
    public Instrumentation instrumentation() { return instrumentation; }
    public void onPanic(Runnable r) { onPanic = r; }

    public synchronized void start(Instrumentation inst) {
        instrumentation = inst;
        if (tickTask != null) return;
        controlServer.start();
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
        pollMenuKey(b, ready);
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

    /**
     * Right Shift opens/closes the menu. The menu is a separate, freshly started JVM
     * process (see eg.gui.StandaloneGui) because this game process is typically launched
     * headless (-Djava.awt.headless=true - Minecraft itself never needs AWT), which blocks
     * any Swing/AWT window from ever working inside it, even after flipping the property
     * back - once the JVM's native windowing subsystem decided at startup it didn't need to
     * initialize, there's no undoing that from inside the same process. A separate process
     * started fresh (not inheriting that flag) has no such problem.
     */
    private void pollMenuKey(GameBridge b, boolean ready) {
        if (!ready) return;
        long handle;
        try { handle = b.windowHandle(); } catch (Throwable t) { handle = 0; }
        if (handle == 0) return;
        boolean down;
        try { down = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS; }
        catch (Throwable t) { return; }
        if (down && !menuKeyWasDown) toggleMenu();
        menuKeyWasDown = down;
    }

    private void toggleMenu() {
        if (menuProcess != null && menuProcess.isAlive()) {
            menuProcess.destroy();
            menuProcess = null;
            return;
        }
        try {
            String javaHome = System.getProperty("java.home");
            String bin = javaHome + File.separator + "bin" + File.separator
                    + (System.getProperty("os.name", "").toLowerCase().contains("win") ? "javaw.exe" : "java");
            String jar = ownJar();
            menuProcess = new ProcessBuilder(bin, "-cp", jar, "eg.gui.StandaloneGui")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
        } catch (Throwable t) {
            log("failed to launch menu process: " + t);
        }
    }

    private static String ownJar() throws URISyntaxException {
        File f = new File(GhostClient.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return f.getAbsolutePath();
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
        controlServer.stop();
        if (menuProcess != null) { menuProcess.destroy(); menuProcess = null; }
    }

    public static void log(String s) { System.err.println("[elytra-ghost] " + s); }
}
