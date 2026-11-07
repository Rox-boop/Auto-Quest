package eg.core;

import eg.bridge.GameBridge;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public enum Category { COMBAT, MOVEMENT }

    public final String name;
    public final String description;
    public final Category category;
    private final List<Setting> settings = new ArrayList<>();
    private volatile boolean enabled;

    protected Module(String name, String description, Category category) {
        this.name = name; this.description = description; this.category = category;
    }

    protected <T extends Setting> T add(T s) { settings.add(s); return s; }
    public List<Setting> settings() { return settings; }
    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean on) {
        if (enabled == on) return;
        enabled = on;
        try { if (on) onEnable(); else onDisable(); }
        catch (Throwable t) { GhostClient.log("module " + name + " toggle failed: " + t); }
    }

    public void toggle() { setEnabled(!enabled); }

    protected void onEnable() {}
    protected void onDisable() {}

    /** Called once per client tick (~20 Hz) while enabled and the bridge is ready. */
    public abstract void onTick(GameBridge game);
}
