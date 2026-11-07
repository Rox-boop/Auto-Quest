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
        List<VirtualMachineDescriptor> vms = VirtualMachine.list();
        if (args.length == 0) {
            System.out.println("PID      DISPLAY NAME");
            for (VirtualMachineDescriptor d : vms)
                System.out.printf("%-8s %s%s%n", d.id(), d.displayName(), looksLikeMinecraft(d.displayName()) ? "   <-- likely" : "");
            System.out.println("\nInject with: java -jar elytra-ghost.jar <pid>");
            return;
        }
        String pid = args[0];
        if (vms.stream().noneMatch(d -> d.id().equals(pid))) {
            System.err.println("PID " + pid + " is not an attachable JVM.");
            System.exit(2);
        }
        String jar = ownJar();
        VirtualMachine vm = VirtualMachine.attach(pid);
        try {
            vm.loadAgent(jar);
            System.out.println("Injected " + jar + " into " + pid);
        } finally {
            vm.detach();
        }
    }

    private static boolean looksLikeMinecraft(String n) {
        String s = n.toLowerCase();
        return s.contains("minecraft") || s.contains("net.fabricmc") || s.contains("cpw.mods")
                || s.contains("lunar") || s.contains("labymod") || s.contains("launchwrapper");
    }

    private static String ownJar() throws URISyntaxException {
        File f = new File(Injector.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!f.isFile()) throw new IllegalStateException("Run from the built jar, not from a class directory: " + f);
        return f.getAbsolutePath();
    }
}
