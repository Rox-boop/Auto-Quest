package eg.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Lets the standalone, separate-process menu (eg.gui.StandaloneGui) see and change module
 * state, without either side touching AWT inside this (headless) game process. Loopback
 * only, one line-based command per round trip, plain text, no auth - anything else on this
 * machine could in principle connect, same as any other localhost-only dev tool; it can only
 * toggle this client's own modules, nothing game- or OS-level beyond what GhostClient itself
 * already exposes.
 *
 * Wire protocol (UTF-8, newline terminated, "." ends a multi-line reply):
 *   LIST                              -> for each module: "MODULE name|description|category|enabled"
 *                                        for each of its settings: "SETTING module|name|kind|value|extra"
 *                                        (kind is BOOL, NUM min,max,step, or MODE mode1,mode2,...)
 *                                        then "."
 *   STATUS                            -> "READY true|false" then the describe() text, then "."
 *   TOGGLE module                     -> "OK" or "ERR message"
 *   SETBOOL module setting true|false -> "OK" or "ERR message"
 *   SETNUM module setting value       -> "OK" or "ERR message"
 *   NEXTMODE module setting           -> "OK" or "ERR message"
 *   PANIC                             -> "OK"
 */
public final class ControlServer {
    public static final int PORT = 47991;

    private volatile ServerSocket server;
    private volatile boolean stopped;

    public synchronized void start() {
        if (server != null) return;
        Thread accept = new Thread(this::acceptLoop, "eg-control-accept");
        accept.setDaemon(true);
        accept.start();
    }

    public synchronized void stop() {
        stopped = true;
        try { if (server != null) server.close(); } catch (IOException ignored) {}
    }

    private void acceptLoop() {
        try {
            server = new ServerSocket(PORT, 4, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            GhostClient.log("ControlServer failed to bind " + PORT + ": " + e);
            return;
        }
        while (!stopped) {
            try {
                Socket s = server.accept();
                Thread t = new Thread(() -> handle(s), "eg-control-client");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (!stopped) GhostClient.log("ControlServer accept failed: " + e);
            }
        }
    }

    private void handle(Socket s) {
        try (s;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8)) {
            String line;
            while ((line = in.readLine()) != null) {
                try {
                    handleCommand(line, out);
                } catch (Throwable t) {
                    out.println("ERR " + t);
                }
            }
        } catch (IOException ignored) {
            // client disconnected
        }
    }

    private void handleCommand(String line, PrintWriter out) {
        String[] p = line.split(" ", 4);
        String cmd = p[0];
        GhostClient gc = GhostClient.get();
        switch (cmd) {
            case "LIST" -> {
                for (Module m : gc.modules()) {
                    out.println("MODULE " + esc(m.name) + "|" + esc(m.description) + "|" + m.category + "|" + m.isEnabled());
                    for (Setting s : m.settings())
                        out.println("SETTING " + esc(m.name) + "|" + esc(s.name) + "|" + settingKind(s) + "|" + settingValue(s) + "|" + settingExtra(s));
                }
                out.println(".");
            }
            case "STATUS" -> {
                boolean ready;
                String desc;
                try { ready = gc.bridge().ready(); } catch (Throwable t) { ready = false; }
                try { desc = gc.bridge().describe(); } catch (Throwable t) { desc = "bridge error: " + t; }
                out.println("READY " + ready);
                out.println(esc(desc));
                out.println(".");
            }
            case "TOGGLE" -> {
                Module m = find(p.length > 1 ? p[1] : "");
                if (m == null) { out.println("ERR no such module"); return; }
                m.toggle();
                out.println("OK");
            }
            case "SETBOOL" -> {
                Setting.Bool b = findSetting(p, Setting.Bool.class);
                if (b == null) { out.println("ERR no such setting"); return; }
                b.set(Boolean.parseBoolean(p[3]));
                out.println("OK");
            }
            case "SETNUM" -> {
                Setting.Num n = findSetting(p, Setting.Num.class);
                if (n == null) { out.println("ERR no such setting"); return; }
                n.set(Double.parseDouble(p[3]));
                out.println("OK");
            }
            case "NEXTMODE" -> {
                Setting.Mode mo = findSetting(p, Setting.Mode.class);
                if (mo == null) { out.println("ERR no such setting"); return; }
                mo.next();
                out.println("OK");
            }
            case "PANIC" -> { gc.panic(); out.println("OK"); }
            default -> out.println("ERR unknown command");
        }
    }

    private Module find(String name) {
        for (Module m : GhostClient.get().modules()) if (m.name.equals(name)) return m;
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends Setting> T findSetting(String[] p, Class<T> type) {
        if (p.length < 4) return null;
        Module m = find(p[1]);
        if (m == null) return null;
        for (Setting s : m.settings())
            if (s.name.equals(p[2]) && type.isInstance(s)) return (T) s;
        return null;
    }

    private static String settingKind(Setting s) {
        if (s instanceof Setting.Bool) return "BOOL";
        if (s instanceof Setting.Num) return "NUM";
        return "MODE";
    }

    private static String settingValue(Setting s) {
        if (s instanceof Setting.Bool b) return String.valueOf(b.get());
        if (s instanceof Setting.Num n) return String.valueOf(n.get());
        return ((Setting.Mode) s).get();
    }

    private static String settingExtra(Setting s) {
        if (s instanceof Setting.Num n) return n.min + "," + n.max + "," + n.step;
        if (s instanceof Setting.Mode mo) return String.join(",", mo.modes);
        return "";
    }

    private static String esc(String s) { return s.replace("|", "/").replace("\n", " "); }
}
