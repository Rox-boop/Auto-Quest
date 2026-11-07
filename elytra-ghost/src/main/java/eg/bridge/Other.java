package eg.bridge;

import eg.core.Vec3;

/** Another player snapshot. {@code center} is the middle of the hitbox. Velocity in blocks per tick. */
public record Other(int id, String name, Vec3 center, Vec3 velocity, float health, boolean gliding) {}
