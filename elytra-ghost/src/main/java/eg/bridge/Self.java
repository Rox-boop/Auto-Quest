package eg.bridge;

import eg.core.Vec3;

/** Local player snapshot. Velocity in blocks per tick. Yaw/pitch in degrees (see Mth). */
public record Self(Vec3 eyePos, Vec3 velocity, float yaw, float pitch,
                   boolean gliding, boolean onGround, float health) {}
