package eg;

import eg.bridge.*;
import eg.core.*;
import eg.modules.*;

import java.util.ArrayList;
import java.util.List;

/** Logic checks without the game. Run via build.sh test. */
public final class SelfTest {
    static int failed;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }
    static boolean near(double a, double b) { return Math.abs(a - b) < 1e-6; }

    static final class Fake implements GameBridge {
        Self self; List<Other> others = new ArrayList<>();
        boolean firework = true, chestElytra = false, chestPlate = true, offTotem = false, key = true;
        int fireworksUsed, glideStarts, totemMoves, chestSwaps; float yaw, pitch; boolean rotSet;
        public boolean ready() { return self != null; }
        public String describe() { return "fake"; }
        public Self self() { return self; }
        public List<Other> others() { return others; }
        public boolean hasInHotbar(Item i) { return firework; }
        public boolean useHotbarItem(Item i) { fireworksUsed++; return true; }
        public boolean chestIs(Item i) { return i == Item.ELYTRA ? chestElytra : chestPlate; }
        public boolean equipChest(Item i) { chestSwaps++; chestElytra = i == Item.ELYTRA; chestPlate = !chestElytra; return true; }
        public boolean offhandIs(Item i) { return offTotem; }
        public boolean moveToOffhand(Item i) { totemMoves++; offTotem = true; return true; }
        public boolean startGliding() { glideStarts++; return true; }
        public void setRotation(float y, float p) { yaw = y; pitch = p; rotSet = true; }
        public boolean attackKeyDown() { return key; }
    }

    static Self me(Vec3 vel, float yaw, float pitch, boolean gliding, boolean ground, float hp) {
        return new Self(new Vec3(0, 100, 0), vel, yaw, pitch, gliding, ground, hp);
    }

    public static void main(String[] a) {
        check("wrapDegrees 190 -> -170", near(Mth.wrapDegrees(190), -170));
        check("wrapDegrees -190 -> 170", near(Mth.wrapDegrees(-190), 170));
        check("wrapDegrees 180 -> -180", near(Mth.wrapDegrees(180), -180));
        double[] r = Mth.rotationTo(Vec3.ZERO, new Vec3(0, 0, 5));
        check("+Z is yaw 0 pitch 0", near(r[0], 0) && near(r[1], 0));
        r = Mth.rotationTo(Vec3.ZERO, new Vec3(-5, 0, 0));
        check("-X is yaw 90", near(r[0], 90));
        r = Mth.rotationTo(Vec3.ZERO, new Vec3(5, 0, 0));
        check("+X is yaw -90", near(r[0], -90));
        r = Mth.rotationTo(Vec3.ZERO, new Vec3(0, 5, 0.0001));
        check("up is pitch ~ -90", r[1] < -89.9);
        Vec3 lv = Mth.lookVector(90, 0);
        check("lookVector(90,0) = -X", near(lv.x(), -1) && near(lv.z(), 0, 1e-9));
        check("angleBetween orthogonal = 90", near(Mth.angleBetween(new Vec3(1, 0, 0), new Vec3(0, 1, 0)), 90));
        check("stepAngle takes short way across wrap", Mth.stepAngle(170, -170, 1.0, 100) > 170);
        check("stepAngle caps at maxStep", near(Mth.stepAngle(0, 90, 1.0, 10), 10));
        check("predict", near(Mth.predict(Vec3.ZERO, new Vec3(1, 0, 0), 2.5).x(), 2.5));

        Setting.Num n = new Setting.Num("x", 5, 0, 10, 0.5); n.set(99); check("Num clamps", near(n.get(), 10));
        n.set(3.26); check("Num snaps to step", near(n.get(), 3.5));

        // AutoFirework
        Fake f = new Fake(); f.self = me(new Vec3(0, 0, 0.4), 0, 0, true, false, 20);
        AutoFirework af = new AutoFirework(); af.setEnabled(true);
        af.onTick(f); check("AutoFirework fires when slow", f.fireworksUsed == 1);
        af.onTick(f); check("AutoFirework respects cooldown", f.fireworksUsed == 1);
        f.self = me(new Vec3(0, 0, 2.0), 0, 0, true, false, 20);
        for (int i = 0; i < 40; i++) af.onTick(f);
        check("AutoFirework idle when fast", f.fireworksUsed == 1);
        f.self = me(new Vec3(0, 0, 0.1), 0, 0, false, false, 20);
        for (int i = 0; i < 40; i++) af.onTick(f);
        check("AutoFirework idle when not gliding", f.fireworksUsed == 1);

        // AutoTotem
        f = new Fake(); f.self = me(Vec3.ZERO, 0, 0, false, true, 20);
        AutoTotem at = new AutoTotem(); at.setEnabled(true);
        at.onTick(f); check("AutoTotem idle at full health", f.totemMoves == 0);
        f.self = me(Vec3.ZERO, 0, 0, false, true, 6);
        at.onTick(f); check("AutoTotem moves at low health", f.totemMoves == 1);
        at.onTick(f); at.onTick(f); at.onTick(f); check("AutoTotem no repeat once equipped", f.totemMoves == 1);

        // ElytraSwap
        f = new Fake(); f.self = me(Vec3.ZERO, 0, 0, false, true, 20);
        f.others.add(new Other(1, "e", new Vec3(0, 100, 4), Vec3.ZERO, 20, false));
        ElytraSwap es = new ElytraSwap(); es.setEnabled(true);
        es.onTick(f); check("ElytraSwap keeps chestplate on ground", f.chestSwaps == 0);
        f.self = me(new Vec3(0, -0.5, 0), 0, 0, false, false, 20);
        es.onTick(f); check("ElytraSwap equips elytra in air", f.chestSwaps == 1 && f.chestElytra);
        for (int i = 0; i < 6; i++) es.onTick(f);
        check("ElytraSwap starts glide when falling", f.glideStarts >= 1);

        // TargetAim
        f = new Fake(); f.self = me(new Vec3(0, 0, 1), 0, 0, true, false, 20);
        f.others.add(new Other(1, "front", new Vec3(-3, 100, 10), Vec3.ZERO, 20, true));
        f.others.add(new Other(2, "behind", new Vec3(0, 100, -10), Vec3.ZERO, 20, true));
        TargetAim ta = new TargetAim(); ta.setEnabled(true);
        ta.onTick(f);
        check("TargetAim turns toward in-FOV target (yaw > 0)", f.rotSet && f.yaw > 0 && f.yaw < 20.0001);
        f.rotSet = false; f.key = false; ta.onTick(f);
        check("TargetAim gated by attack key", !f.rotSet);
        f.key = true; f.self = me(new Vec3(0, 0, 1), 0, 0, false, false, 20); ta.onTick(f);
        check("TargetAim gated by gliding", !f.rotSet);
        f.others.clear(); f.others.add(new Other(3, "nan", new Vec3(Double.NaN, 0, 0), Vec3.ZERO, 20, true));
        f.self = me(new Vec3(0, 0, 1), 0, 0, true, false, 20); ta.onTick(f);
        check("TargetAim ignores non-finite target", !f.rotSet);

        // GhostClient: crash isolation and panic
        GhostClient gc = GhostClient.get();
        Fake g2 = new Fake(); g2.self = me(new Vec3(0, 0, 0.1), 0, 0, true, false, 20) ;
        gc.setBridge(g2);
        gc.modules().get(2).setEnabled(true);
        gc.tick();
        check("GhostClient tick drives modules", g2.fireworksUsed == 1);
        gc.panic();
        check("panic disables all", gc.modules().stream().noneMatch(eg.core.Module::isEnabled));
        gc.setBridge(null); gc.tick();
        check("unresolved bridge is never ready", !gc.bridge().ready());

        System.out.println(failed == 0 ? "ALL PASSED" : failed + " FAILED");
        System.exit(failed == 0 ? 0 : 1);
    }

    static boolean near(double a, double b, double eps) { return Math.abs(a - b) < eps; }
}
