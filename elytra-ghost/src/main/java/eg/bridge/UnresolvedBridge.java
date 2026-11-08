package eg.bridge;

import java.util.List;

/** Default until a version-specific bridge is installed. Nothing here pretends to work. */
public final class UnresolvedBridge implements GameBridge {
    @Override public boolean ready() { return false; }
    @Override public String describe() {
        return "No game bridge installed. Minecraft version/loader/mappings still unknown, so no game access exists yet.";
    }
    @Override public Self self() { return null; }
    @Override public List<Other> others() { return List.of(); }
    @Override public boolean hasInHotbar(Item i) { return false; }
    @Override public boolean useHotbarItem(Item i) { return false; }
    @Override public boolean chestIs(Item i) { return false; }
    @Override public boolean equipChest(Item i) { return false; }
    @Override public boolean offhandIs(Item i) { return false; }
    @Override public boolean moveToOffhand(Item i) { return false; }
    @Override public boolean startGliding() { return false; }
    @Override public void setRotation(float yaw, float pitch) {}
    @Override public boolean attackKeyDown() { return false; }
    @Override public long windowHandle() { return 0; }
}
