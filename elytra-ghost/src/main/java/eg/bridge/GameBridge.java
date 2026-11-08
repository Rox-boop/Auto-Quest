package eg.bridge;

import java.util.List;

/**
 * The only place that touches Minecraft internals. Everything else is version independent.
 * Contract:
 *  - called from the ticker thread; implementations must marshal onto the game thread
 *    where the game requires it (inventory, packets, rotation writes);
 *  - velocities are blocks per tick, angles are degrees in Minecraft convention;
 *  - action methods return true only if the action was actually issued.
 */
public interface GameBridge {
    /** False while no world / local player exists or mappings are not resolved. */
    boolean ready();
    /** Human readable state for the GUI (version, loader, what is missing). */
    String describe();

    Self self();
    List<Other> others();

    boolean hasInHotbar(Item item);
    /** Selects the hotbar slot holding the item, uses it, restores the previous slot. */
    boolean useHotbarItem(Item item);

    boolean chestIs(Item item);
    boolean equipChest(Item item);

    boolean offhandIs(Item item);
    boolean moveToOffhand(Item item);

    /** Starts elytra gliding (same effect as jump-in-air). */
    boolean startGliding();
    void setRotation(float yaw, float pitch);
    boolean attackKeyDown();

    /**
     * The game window's native GLFW handle (see org.lwjgl.glfw.GLFW), or 0 if unknown/not
     * resolvable for this bridge. Used only to poll a raw key state (e.g. Right Shift to
     * open the external menu) with GLFW directly - LWJGL's own classes are the same for
     * every launcher, only the Minecraft object that owns the Window differs per bridge.
     */
    long windowHandle();
}
