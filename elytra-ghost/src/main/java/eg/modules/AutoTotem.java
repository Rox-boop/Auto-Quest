package eg.modules;

import eg.bridge.GameBridge;
import eg.bridge.Item;
import eg.bridge.Self;
import eg.core.Module;
import eg.core.Setting;

public final class AutoTotem extends Module {
    private final Setting.Num health = add(new Setting.Num("Health <=", 12, 1, 20, 1));
    private final Setting.Num delay = add(new Setting.Num("Delay ticks", 2, 0, 20, 1));
    private final Setting.Bool always = add(new Setting.Bool("Always keep totem", false));
    private int cooldown;

    public AutoTotem() { super("AutoTotem", "Keeps a totem in the offhand", Category.COMBAT); }

    @Override protected void onEnable() { cooldown = 0; }

    @Override public void onTick(GameBridge g) {
        if (cooldown > 0) { cooldown--; return; }
        Self me = g.self();
        if (me == null) return;
        if (!always.get() && me.health() > health.get()) return;
        if (g.offhandIs(Item.TOTEM_OF_UNDYING)) return;
        if (g.moveToOffhand(Item.TOTEM_OF_UNDYING)) cooldown = delay.getInt();
    }
}
