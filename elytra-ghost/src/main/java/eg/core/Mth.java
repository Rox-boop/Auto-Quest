package eg.core;

/**
 * Pure math, no game access. Minecraft rotation convention:
 * yaw 0 = +Z (south), yaw 90 = -X (west), degrees; pitch -90 = straight up, +90 = straight down.
 */
public final class Mth {
    private Mth() {}

    public static double wrapDegrees(double d) {
        d %= 360.0;
        if (d >= 180.0) d -= 360.0;
        if (d < -180.0) d += 360.0;
        return d;
    }

    public static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }

    /** {yaw, pitch} in degrees to look from {@code from} towards {@code to}. */
    public static double[] rotationTo(Vec3 from, Vec3 to) {
        Vec3 d = to.sub(from);
        double yaw = Math.toDegrees(Math.atan2(-d.x(), d.z()));
        double pitch = -Math.toDegrees(Math.atan2(d.y(), d.horizontalLength()));
        return new double[] { wrapDegrees(yaw), pitch };
    }

    /** Unit look vector for yaw/pitch (degrees). */
    public static Vec3 lookVector(double yaw, double pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        double cp = Math.cos(p);
        return new Vec3(-Math.sin(y) * cp, -Math.sin(p), Math.cos(y) * cp);
    }

    /** Angle in degrees between two vectors; 180 if either has zero length. */
    public static double angleBetween(Vec3 a, Vec3 b) {
        double la = a.length(), lb = b.length();
        if (la < 1e-9 || lb < 1e-9) return 180.0;
        double dot = (a.x() * b.x() + a.y() * b.y() + a.z() * b.z()) / (la * lb);
        return Math.toDegrees(Math.acos(clamp(dot, -1.0, 1.0)));
    }

    /**
     * One smoothing step on a wrapped angle: moves {@code factor} (0..1) of the remaining
     * delta, capped to {@code maxStep} degrees. Returns the new angle (not wrapped).
     */
    public static double stepAngle(double current, double target, double factor, double maxStep) {
        double delta = wrapDegrees(target - current);
        double step = clamp(delta * clamp(factor, 0.0, 1.0), -Math.abs(maxStep), Math.abs(maxStep));
        return current + step;
    }

    /** Linear lead: position after {@code ticks} ticks at constant velocity (blocks/tick). */
    public static Vec3 predict(Vec3 pos, Vec3 velocity, double ticks) { return pos.add(velocity.scale(ticks)); }
}
