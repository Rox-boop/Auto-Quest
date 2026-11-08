package eg.agent;

import eg.bridge.BridgeLoader;
import eg.core.GhostClient;
import eg.gui.ClickGui;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Agent {
    private static final AtomicBoolean LOADED = new AtomicBoolean();

    private Agent() {}

    /** Entry point when attached to a running JVM. */
    public static void agentmain(String args, Instrumentation inst) { boot(inst); }
    /** Entry point with -javaagent at launch. */
    public static void premain(String args, Instrumentation inst) { boot(inst); }

    private static void boot(Instrumentation inst) {
        if (!LOADED.compareAndSet(false, true)) { GhostClient.log("already loaded"); return; }
        try {
            // Some launchers (seen with Prism Launcher) start Minecraft with
            // -Djava.awt.headless=true, since the game itself never touches java.awt (it
            // renders through LWJGL/GLFW, not AWT). AWT/Swing only latch that flag's value
            // into a static field the FIRST time anything touches GraphicsEnvironment, so
            // setting it back to false here - before ClickGui or anything else is the first
            // thing in this JVM to touch AWT - still takes effect. If something earlier in
            // this same JVM already initialized AWT as headless, this does nothing and
            // ClickGui.open() will log and crash-file the same HeadlessException as before.
            System.setProperty("java.awt.headless", "false");
            BridgeLoader.tryInstall();
            GhostClient.get().start(inst);
            ClickGui.open();
            GhostClient.log("loaded");
        } catch (Throwable t) {
            GhostClient.log("init failed: " + t);
            GhostClient.get().shutdown();
            LOADED.set(false);
        }
    }
}
