package eg.modules;

import eg.bridge.GameBridge;
import eg.bridge.Other;
import eg.bridge.Self;
import eg.core.Module;
import eg.core.Mth;
import eg.core.Setting;
import eg.core.Vec3;

/** Smoothed aim assist with velocity lead, FOV limit and key gating. Values are uncalibrated defaults. */
public final class TargetAim extends Module {
    private final Setting.Num range = add(new Setting.Num("Range", 20, 4, 60, 1));
    private final Setting.Num fov = add(new Setting.Num("FOV", 60, 10, 180, 1));
    private final Setting.Num smooth = add(new Setting.Num("Smoothing %", 25, 1, 100, 1));
    private final Setting.Num maxStep = add(new Setting.Num("Max deg/tick", 20, 1, 60, 1));
    private final Setting.Num lead = add(new Setting.Num("Lead ticks", 2, 0, 10, 0.5));
    private final Setting.Bool onlyGliding = add(new Setting.Bool("Only gliding", true));
    private final Setting.Bool needKey = add(new Setting.Bool("Require attack key", true));

    public TargetAim() { super("TargetAim", "Smooth aim assist on the best target in your FOV", Category.COMBAT); }

    @Override public void onTick(GameBridge g) {
        Self me = g.self();
        if (me == null || !me.eyePos().isFinite()) return;
        if (onlyGliding.get() && !me.gliding()) return;
        if (needKey.get() && !g.attackKeyDown()) return;

        Vec3 look = Mth.lookVector(me.yaw(), me.pitch());
        Vec3 best = null;
        double bestAngle = fov.get() / 2.0;
        for (Other o : g.others()) {
            if (o.health() <= 0 || !o.center().isFinite() || !o.velocity().isFinite()) continue;
            Vec3 aim = Mth.predict(o.center(), o.velocity(), lead.get());
            Vec3 d = aim.sub(me.eyePos());
            if (d.length() > range.get() || d.length() < 0.5) continue;
            double a = Mth.angleBetween(look, d);
            if (a <= bestAngle) { bestAngle = a; best = aim; }
        }
        if (best == null) return;

        double[] want = Mth.rotationTo(me.eyePos(), best);
        double f = smooth.get() / 100.0;
        double yaw = Mth.stepAngle(me.yaw(), want[0], f, maxStep.get());
        double pitch = Mth.clamp(Mth.stepAngle(me.pitch(), want[1], f, maxStep.get()), -90, 90);
        g.setRotation((float) yaw, (float) pitch);
    }
}
