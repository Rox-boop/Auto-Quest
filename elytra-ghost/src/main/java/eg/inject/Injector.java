package eg.inject;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Attaches this jar as a Java agent to a running JVM.
 * Needs a JDK (jdk.attach module), the same OS user as the target, and a target that
 * does not run with -XX:+DisableAttachMechanism.
 *
 *   java -jar elytra-ghost.jar            lists JVMs
 *   java -jar elytra-ghost.jar &lt;pid&gt;     injects
 */
public final class Injector {
    private Injector() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            // Double-clicked, or run with no arguments: GUI if a display is available,
            // otherwise fall back to the old CLI listing (e.g. over SSH with no X server).
            boolean headless = java.awt.GraphicsEnvironment.isHeadless();
            if (!headless) { eg.inject.InjectorGui.open(); return; }
            List<VirtualMachineDescriptor> vms = VirtualMachine.list();
            System.out.println("PID      DISPLAY NAME");
            for (VirtualMachineDescriptor d : vms)
                System.out.printf("%-8s %s%s%n", d.id(), d.displayName(), looksLikeMinecraft(d.displayName()) ? "   <-- likely" : "");
            System.out.println("\nInject with: java -jar elytra-ghost.jar <pid>");
            return;
        }
        String pid = args[0];
        inject(pid);
        System.out.println("Injected " + ownJar() + " into " + pid);
    }

    /** Attaches this jar as an agent to the given PID. Throws on failure; does not print or exit. */
    static void inject(String pid) throws Exception {
        List<VirtualMachineDescriptor> vms = VirtualMachine.list();
        if (vms.stream().noneMatch(d -> d.id().equals(pid)))
            throw new IllegalArgumentException("PID " + pid + " is not an attachable JVM.");
        String jar = ownJar();
        VirtualMachine vm = VirtualMachine.attach(pid);
        try {
            vm.loadAgent(jar);
        } finally {
            vm.detach();
        }
    }

    static List<VirtualMachineDescriptor> listVms() { return VirtualMachine.list(); }

    static boolean looksLikeMinecraft(String n) {
        String s = n.toLowerCase();
        return s.contains("minecraft") || s.contains("net.fabricmc") || s.contains("cpw.mods")
                || s.contains("lunar") || s.contains("labymod") || s.contains("launchwrapper")
                || s.contains("prismlauncher") || s.contains("multimc") || s.contains("modrinth");
    }

    static String ownJar() throws URISyntaxException {
        File f = new File(Injector.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!f.isFile()) throw new IllegalStateException("Run from the built jar, not from a class directory: " + f);
        return f.getAbsolutePath();
    }
}
