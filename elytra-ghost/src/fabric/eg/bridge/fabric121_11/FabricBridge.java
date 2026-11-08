package eg.bridge.fabric121_11;

import eg.bridge.GameBridge;
import eg.bridge.Item;
import eg.bridge.Other;
import eg.bridge.Self;
import eg.core.GhostClient;
import eg.core.Vec3;

import net.minecraft.class_310; // Minecraft
import net.minecraft.class_315; // Options
import net.minecraft.class_304; // KeyMapping
import net.minecraft.class_638; // ClientLevel
import net.minecraft.class_746; // LocalPlayer
import net.minecraft.class_742; // AbstractClientPlayer
import net.minecraft.class_1661; // Inventory
import net.minecraft.class_1304; // EquipmentSlot
import net.minecraft.class_1799; // ItemStack
import net.minecraft.class_1792; // Item
import net.minecraft.class_1802; // Items
import net.minecraft.class_243; // Vec3
import net.minecraft.class_238; // AABB
import net.minecraft.class_7923; // BuiltInRegistries
import net.minecraft.class_7922; // DefaultedRegistry
import net.minecraft.class_2960; // Identifier
import net.minecraft.class_1041; // Window

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * GameBridge for Minecraft 1.21.11 under the Fabric loader. See {@link Mapping} for the
 * full name table and how each member was confirmed. Lives in a normal named package,
 * unlike the vanilla bridge: Fabric's intermediary names keep a real {@code net.minecraft}
 * package, so there is no unnamed-package import restriction to work around here.
 *
 * Same read/write split as the vanilla bridge, and for the same reasons (see its javadoc):
 * all reads are solid, setRotation is solid, useHotbarItem/moveToOffhand/startGliding work
 * by pulsing the real KeyMapping the corresponding key press would set, and equipChest()
 * is gated off because it would need a verified inventory container-click packet.
 *
 * One more thing is gated here that was NOT gated in the vanilla bridge: {@link #others()}.
 * ClientLevel.players() could not be matched directly in Fabric's intermediary mappings for
 * this build - it is one of the methods Entity/ClientLevel get from an interface, and the
 * one interface candidate found for it in the tiny file could not be confirmed as the right
 * one without actually running Fabric's remapper (blocked in this session - see Mapping.java
 * for exactly what was checked). Guessing it risks a silent wrong-target bug (aiming or
 * "enemy nearby" checks acting on an unrelated list), which is worse than admitting it is
 * missing, so {@link #others()} returns an empty list. This disables TargetAim entirely
 * under Fabric and makes ElytraSwap's "enemy nearby, keep chestplate on" check always false
 * (it still swaps to elytra in the air and auto-glides). Fix: run tiny-remapper locally
 * against this build's client.jar with the intermediary-1.21.11 mappings, then javap the
 * real output class_638 to read off players()'s actual method_ number directly.
 */
public final class FabricBridge implements GameBridge {
    private final ScheduledExecutorService keyTimer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "eg-keypulse-fabric");
        t.setDaemon(true);
        return t;
    });

    @Override
    public boolean ready() {
        try {
            class_310 mc = class_310.method_1551();
            return mc != null && mc.field_1687 != null && mc.field_1724 != null;
        } catch (LinkageError wrongBuild) {
            throw wrongBuild; // see BridgeLoader: forces this at install time on purpose
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String describe() {
        try {
            class_310 mc = class_310.method_1551();
            if (mc == null) return "Minecraft.getInstance() returned null";
            if (mc.field_1687 == null) return "no ClientLevel - not in a world";
            if (mc.field_1724 == null) return "no LocalPlayer - not in a world";
            return "1.21.11 Fabric bridge ready (others()/ESP disabled, see Mapping.java)";
        } catch (Throwable t) {
            return "bridge error: " + t;
        }
    }

    private class_310 mc() { return class_310.method_1551(); }

    @Override
    public Self self() {
        try {
            class_746 p = mc().field_1724;
            if (p == null) return null;
            return new Self(vec(p.method_33571()), vec(p.method_18798()), p.method_36454(),
                    p.method_36455(), p.method_6128(), p.method_24828(), p.method_6032());
        } catch (Throwable t) {
            GhostClient.log("FabricBridge.self() failed: " + t);
            return null;
        }
    }

    @Override
    public List<Other> others() {
        // See class javadoc: ClientLevel.players() is not safely resolvable from this
        // session's static mapping analysis alone. Intentionally empty, not guessed.
        return new ArrayList<>();
    }

    @Override
    public boolean hasInHotbar(Item item) {
        return findHotbarSlot(item) >= 0;
    }

    @Override
    public boolean useHotbarItem(Item item) {
        if (item != Item.FIREWORK_ROCKET) return false;
        int slot = findHotbarSlot(item);
        if (slot < 0) return false;
        return withSelectedSlot(slot, () -> pulseKey(mc().field_1690.field_1904)); // keyUse
    }

    @Override
    public boolean chestIs(Item item) {
        try {
            class_746 p = mc().field_1724;
            if (p == null) return false;
            class_1799 chest = p.method_6118(class_1304.field_6174); // getItemBySlot(CHEST)
            if (chest.method_7960()) return false;
            return matchesItem(chest, item);
        } catch (Throwable t) {
            GhostClient.log("FabricBridge.chestIs() failed: " + t);
            return false;
        }
    }

    @Override
    public boolean equipChest(Item item) {
        return false; // see class javadoc and VanillaBridge's javadoc for why
    }

    @Override
    public boolean offhandIs(Item item) {
        try {
            class_746 p = mc().field_1724;
            if (p == null) return false;
            class_1799 off = p.method_6118(class_1304.field_6171); // getItemBySlot(OFFHAND)
            return !off.method_7960() && matchesItem(off, item);
        } catch (Throwable t) {
            GhostClient.log("FabricBridge.offhandIs() failed: " + t);
            return false;
        }
    }

    @Override
    public boolean moveToOffhand(Item item) {
        if (item != Item.TOTEM_OF_UNDYING) return false;
        int slot = findHotbarSlot(item);
        if (slot < 0) return false;
        return withSelectedSlot(slot, () -> pulseKey(mc().field_1690.field_1831)); // keySwapOffhand
    }

    @Override
    public boolean startGliding() {
        try {
            class_746 p = mc().field_1724;
            if (p == null || p.method_24828() /* onGround */) return false;
            pulseKey(mc().field_1690.field_1903); // keyJump
            return true;
        } catch (Throwable t) {
            GhostClient.log("FabricBridge.startGliding() failed: " + t);
            return false;
        }
    }

    @Override
    public void setRotation(float yaw, float pitch) {
        try {
            class_746 p = mc().field_1724;
            if (p == null) return;
            p.method_36456(yaw);   // setYRot
            p.method_36457(pitch); // setXRot
        } catch (Throwable t) {
            GhostClient.log("FabricBridge.setRotation() failed: " + t);
        }
    }

    @Override
    public boolean attackKeyDown() {
        try {
            return mc().field_1690.field_1886.method_1434(); // keyAttack.isDown()
        } catch (Throwable t) {
            return false;
        }
    }

    private java.lang.reflect.Field windowField; // class_310.field_1704 (Window) - private, cached reflective access

    @Override
    public long windowHandle() {
        try {
            if (windowField == null) {
                windowField = class_310.class.getDeclaredField("field_1704");
                windowField.setAccessible(true);
            }
            class_1041 window = (class_1041) windowField.get(mc());
            return window == null ? 0 : window.method_4490(); // Window.handle()
        } catch (Throwable t) {
            GhostClient.log("FabricBridge.windowHandle() failed: " + t);
            return 0;
        }
    }

    // --- helpers ---

    private int findHotbarSlot(Item item) {
        try {
            class_1661 inv = mc().field_1724.method_31548();
            for (int i = 0; i < 9; i++) { // vanilla hotbar is always slots 0-8
                class_1799 stack = inv.method_5438(i);
                if (!stack.method_7960() && matchesItem(stack, item)) return i;
            }
        } catch (Throwable t) {
            GhostClient.log("FabricBridge.findHotbarSlot() failed: " + t);
        }
        return -1;
    }

    private boolean matchesItem(class_1799 stack, Item item) {
        try {
            class_1792 actual = stack.method_7909();
            switch (item) {
                case ELYTRA: return actual == class_1802.field_8833;
                case FIREWORK_ROCKET: return actual == class_1802.field_8639;
                case TOTEM_OF_UNDYING: return actual == class_1802.field_8288;
                case CHESTPLATE: return registryPath(actual).endsWith("_chestplate"); // heuristic
                default: return false;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    private String registryPath(class_1792 item) {
        try {
            Object key = class_7923.field_41178.method_10221(item); // getKey(Object)
            return ((class_2960) key).method_12832(); // getPath()
        } catch (Throwable t) {
            return "";
        }
    }

    private interface KeyAction { void run(); }

    private boolean withSelectedSlot(int slot, KeyAction action) {
        try {
            class_1661 inv = mc().field_1724.method_31548();
            int previous = inv.method_67532();
            inv.method_61496(slot);
            action.run();
            keyTimer.schedule(() -> {
                try { inv.method_61496(previous); } catch (Throwable t) { GhostClient.log("restore hotbar slot failed: " + t); }
            }, 120, TimeUnit.MILLISECONDS);
            return true;
        } catch (Throwable t) {
            GhostClient.log("FabricBridge.withSelectedSlot() failed: " + t);
            return false;
        }
    }

    private void pulseKey(class_304 key) {
        key.method_23481(true);
        keyTimer.schedule(() -> {
            try { key.method_23481(false); } catch (Throwable t) { GhostClient.log("key release failed: " + t); }
        }, 100, TimeUnit.MILLISECONDS);
    }

    private static Vec3 vec(class_243 v) { return new Vec3(v.field_1352, v.field_1351, v.field_1350); }
}
