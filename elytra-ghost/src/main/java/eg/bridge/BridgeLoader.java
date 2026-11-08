package eg.bridge;

import eg.core.GhostClient;

/**
 * Tries each known version/launcher bridge in turn and installs the first one whose target
 * classes are actually present in this JVM. Outside a matching Minecraft process (including
 * our own tests), every lookup fails and {@link UnresolvedBridge} stays installed.
 */
public final class BridgeLoader {
    private BridgeLoader() {}

    /** Fully-qualified names of GameBridge implementations to try, most specific first. */
    private static final String[] CANDIDATES = {
        "VanillaBridge", // Minecraft 1.21.11, official vanilla launcher (unnamed package - see its javadoc)
        "eg.bridge.fabric121_11.FabricBridge" // Minecraft 1.21.11 under Fabric loader - only
        // present in the jar if built with tools/build-fabric-bridge.sh (see that script
        // and src/fabric/eg/bridge/fabric121_11/Mapping.java for why it's a separate build)
    };

    public static void tryInstall() {
        for (String className : CANDIDATES) {
            try {
                Class<?> c = Class.forName(className);
                Object instance = c.getDeclaredConstructor().newInstance();
                if (!(instance instanceof GameBridge b)) continue;
                // Construction alone proves nothing: the JVM resolves a class's symbolic
                // references to other classes (gfj, hnh, ...) lazily, per method, not at
                // load time. Calling ready() is what actually forces that resolution, so a
                // bridge for the wrong build throws here (LinkageError), not above.
                b.ready();
                GhostClient.get().setBridge(b);
                GhostClient.log("installed bridge: " + className);
                return;
            } catch (ClassNotFoundException | LinkageError notThisGame) {
                // expected when this JVM isn't the matching Minecraft build/launcher:
                // ClassNotFoundException if our bridge class itself isn't here, LinkageError
                // (e.g. NoClassDefFoundError) if it's here but the game classes it depends on
                // (gfj, hnh, ...) aren't - i.e. this bridge is for the wrong build.
            } catch (Throwable t) {
                GhostClient.log("bridge candidate " + className + " present but failed to load: " + t);
            }
        }
        GhostClient.log("no matching bridge found, staying on UnresolvedBridge");
    }
}
