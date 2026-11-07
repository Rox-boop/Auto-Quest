package eg.modules;

import eg.bridge.GameBridge;
import eg.bridge.Item;
import eg.bridge.Self;
import eg.core.Module;
import eg.core.Setting;

/** Fires a rocket from the hotbar when gliding speed drops below a threshold. Defaults uncalibrated. */
public final class AutoFirework extends Module {
    private final Setting.Num minSpeed = add(new Setting.Num("Min speed b/t", 1.0, 0.1, 3.0, 0.05));
    private final Setting.Num cooldownTicks = add(new Setting.Num("Cooldown ticks", 15, 3, 60, 1));
    private int cooldown;

    public AutoFirework() { super("AutoFirework", "Keeps elytra speed up with rockets", Category.MOVEMENT); }

    @Override protected void onEnable() { cooldown = 0; }

    @Override public void onTick(GameBridge g) {
        if (cooldown > 0) { cooldown--; return; }
        Self me = g.self();
        if (me == null || !me.gliding() || !me.velocity().isFinite()) return;
        if (me.velocity().length() >= minSpeed.get()) return;
        if (!g.hasInHotbar(Item.FIREWORK_ROCKET)) return;
        if (g.useHotbarItem(Item.FIREWORK_ROCKET)) cooldown = cooldownTicks.getInt();
    }
}
