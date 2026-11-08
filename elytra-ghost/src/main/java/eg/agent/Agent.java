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
