package eg.modules;

import eg.bridge.GameBridge;
import eg.bridge.Item;
import eg.bridge.Other;
import eg.bridge.Self;
import eg.core.Module;
import eg.core.Setting;

/** Chestplate on the ground near enemies, elytra in the air; optional auto glide start. */
public final class ElytraSwap extends Module {
    private final Setting.Num enemyRange = add(new Setting.Num("Enemy range", 8, 3, 30, 1));
    private final Setting.Num delay = add(new Setting.Num("Delay ticks", 4, 1, 20, 1));
    private final Setting.Bool autoGlide = add(new Setting.Bool("Auto glide", true));
    private int cooldown;

    public ElytraSwap() { super("ElytraSwap", "Swaps chestplate and elytra by situation", Category.MOVEMENT); }

    @Override protected void onEnable() { cooldown = 0; }

    @Override public void onTick(GameBridge g) {
        if (cooldown > 0) { cooldown--; return; }
        Self me = g.self();
        if (me == null) return;

        if (me.onGround()) {
            if (enemyNear(g, me) && !g.chestIs(Item.CHESTPLATE) && g.equipChest(Item.CHESTPLATE))
                cooldown = delay.getInt();
            return;
        }
        if (!g.chestIs(Item.ELYTRA)) {
            if (g.equipChest(Item.ELYTRA)) cooldown = delay.getInt();
            return;
        }
        if (autoGlide.get() && !me.gliding() && me.velocity().y() < -0.1 && g.startGliding())
            cooldown = delay.getInt();
    }

    private boolean enemyNear(GameBridge g, Self me) {
        for (Other o : g.others())
            if (o.health() > 0 && o.center().sub(me.eyePos()).length() <= enemyRange.get()) return true;
        return false;
    }
}
