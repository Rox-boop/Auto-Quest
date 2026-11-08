// No package statement: the obfuscated vanilla client classes (gfj, hnh, ftm, ...) live in
// Java's unnamed/default package, and a named package is not allowed to import a type from
// the unnamed package. Living in the unnamed package too lets this file reference them
// directly, with no import needed for same-package types. See Mapping.java for what each
// short name is. eg.* types are in named packages, so importing those works normally.

import eg.bridge.GameBridge;
import eg.bridge.Item;
import eg.bridge.Other;
import eg.bridge.Self;
import eg.core.GhostClient;
import eg.core.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * GameBridge for Minecraft 1.21.11, official vanilla launcher. See {@link Mapping} for the
 * full obfuscated-name table and how each member was confirmed.
 *
 * What is solid here:
 *  - all read access (position, velocity, rotation, health, gliding, on-ground, equipment,
 *    hotbar contents, other players, key state): direct field/method access, no guessing.
 *  - setRotation: writes Entity.yRot/xRot directly - the same fields the client's own
 *    render/movement code reads, so this is the standard, correct way an external aim
 *    module turns the local player.
 *  - useHotbarItem(FIREWORK_ROCKET) and moveToOffhand(TOTEM_OF_UNDYING): select the hotbar
 *    slot, then pulse the SAME KeyMapping the real key press would set
 *    (Options.keyUse / Options.keySwapOffhand). Minecraft's own input handling turns a
 *    KeyMapping press into the correct server packet on its own tick, so this does not
 *    need a hand-built packet and does not desync.
 *  - startGliding(): pulses Options.keyJump while falling, exactly how a real player
 *    triggers elytra flight. Same reasoning as above.
 *
 * What is NOT solid, on purpose:
 *  - equipChest(...): vanilla has no keybind for "wear this armor piece from the hotbar"
 *    while playing. Doing it for real needs a ServerboundContainerClickPacket matching
 *    this version's exact constructor, which was not extracted or verified in this
 *    session. Faking it with a local setItemSlot() call would change only the client's own
 *    rendering state - the server never finds out, so it gets silently corrected back
 *    (the inventory is server-authoritative) and the elytra/chestplate never actually
 *    swaps on the server. So this method is left returning {@code false}, gated, rather
 *    than pretending to work. Fix: capture a real client-to-server inventory click in a
 *    packet logger or Wireshark/mitm, read its exact field layout, then implement for
 *    real against that.
 */
public final class VanillaBridge implements GameBridge {
    private final ScheduledExecutorService keyTimer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "eg-keypulse");
        t.setDaemon(true);
        return t;
    });

    @Override
    public boolean ready() {
        try {
            gfj mc = gfj.V();
            return mc != null && mc.r != null && mc.s != null;
        } catch (LinkageError wrongBuild) {
            // Propagate: BridgeLoader calls ready() once at install time specifically to
            // force resolution of gfj/hnh/... and find out whether this bridge matches the
            // running game. Swallowing this here would hide a wrong-build mismatch and
            // leave this bridge installed uselessly instead of falling back to Unresolved.
            throw wrongBuild;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String describe() {
        try {
            gfj mc = gfj.V();
            if (mc == null) return "Minecraft.getInstance() returned null";
            if (mc.r == null) return "no ClientLevel - not in a world";
            if (mc.s == null) return "no LocalPlayer - not in a world";
            return "1.21.11 vanilla bridge ready, " + mc.r.E().size() + " player(s) tracked";
        } catch (Throwable t) {
            return "bridge error: " + t;
        }
    }

    private gfj mc() { return gfj.V(); }

    @Override
    public Self self() {
        try {
            hnh p = mc().s;
            if (p == null) return null;
            return new Self(vec(p.bV()), vec(p.dN()), p.ec(), p.ee(), p.gj(), p.aV(), p.eZ());
        } catch (Throwable t) {
            GhostClient.log("VanillaBridge.self() failed: " + t);
            return null;
        }
    }

    @Override
    public List<Other> others() {
        List<Other> out = new ArrayList<>();
        try {
            hnh me = mc().s;
            hif level = mc().r;
            if (me == null || level == null) return out;
            for (hne o : level.E()) {
                if (o == null || o == me) continue;
                try {
                    fth box = o.dj();
                    Vec3 center = new Vec3((box.a + box.d) / 2.0, (box.b + box.e) / 2.0, (box.c + box.f) / 2.0);
                    out.add(new Other(o.aA(), o.da(), center, vec(o.dN()), o.eZ(), o.gj()));
                } catch (Throwable inner) {
                    GhostClient.log("VanillaBridge.others() skipped one entity: " + inner);
                }
            }
        } catch (Throwable t) {
            GhostClient.log("VanillaBridge.others() failed: " + t);
        }
        return out;
    }

    @Override
    public boolean hasInHotbar(Item item) {
        return findHotbarSlot(item) >= 0;
    }

    @Override
    public boolean useHotbarItem(Item item) {
        if (item != Item.FIREWORK_ROCKET) return false; // only verified for rockets
        int slot = findHotbarSlot(item);
        if (slot < 0) return false;
        return withSelectedSlot(slot, () -> pulseKey(mc().k.C));
    }

    @Override
    public boolean chestIs(Item item) {
        try {
            hnh p = mc().s;
            if (p == null) return false;
            dlt chest = p.a(cgv.e); // getItemBySlot(CHEST)
            if (chest.f()) return false; // empty slot matches nothing
            return matchesItem(chest, item);
        } catch (Throwable t) {
            GhostClient.log("VanillaBridge.chestIs() failed: " + t);
            return false;
        }
    }

    @Override
    public boolean equipChest(Item item) {
        // See class javadoc: not implemented for real, would only desync. Gated.
        return false;
    }

    @Override
    public boolean offhandIs(Item item) {
        try {
            hnh p = mc().s;
            if (p == null) return false;
            dlt off = p.a(cgv.b); // getItemBySlot(OFFHAND)
            return !off.f() && matchesItem(off, item);
        } catch (Throwable t) {
            GhostClient.log("VanillaBridge.offhandIs() failed: " + t);
            return false;
        }
    }

    @Override
    public boolean moveToOffhand(Item item) {
        if (item != Item.TOTEM_OF_UNDYING) return false; // only verified for totems
        int slot = findHotbarSlot(item);
        if (slot < 0) return false;
        return withSelectedSlot(slot, () -> pulseKey(mc().k.A));
    }

    @Override
    public boolean startGliding() {
        try {
            hnh p = mc().s;
            if (p == null || p.aV() /* onGround */) return false;
            pulseKey(mc().k.w); // keyJump
            return true;
        } catch (Throwable t) {
            GhostClient.log("VanillaBridge.startGliding() failed: " + t);
            return false;
        }
    }

    @Override
    public void setRotation(float yaw, float pitch) {
        try {
            hnh p = mc().s;
            if (p == null) return;
            p.v(yaw);   // setYRot
            p.w(pitch); // setXRot
        } catch (Throwable t) {
            GhostClient.log("VanillaBridge.setRotation() failed: " + t);
        }
    }

    @Override
    public boolean attackKeyDown() {
        try {
            return mc().k.D.f(); // keyAttack.isDown()
        } catch (Throwable t) {
            return false;
        }
    }

    // --- helpers ---

    private int findHotbarSlot(Item item) {
        try {
            ddl inv = mc().s.gK();
            int hotbarSize = 9; // vanilla hotbar is always slots 0-8
            for (int i = 0; i < hotbarSize && i < inv.b(); i++) {
                dlt stack = inv.a(i);
                if (!stack.f() && matchesItem(stack, item)) return i;
            }
        } catch (Throwable t) {
            GhostClient.log("VanillaBridge.findHotbarSlot() failed: " + t);
        }
        return -1;
    }

    private boolean matchesItem(dlt stack, Item item) {
        try {
            dlp actual = stack.h(); // getItem()
            switch (item) {
                case ELYTRA: return actual == dlx.pq;
                case FIREWORK_ROCKET: return actual == dlx.wG;
                case TOTEM_OF_UNDYING: return actual == dlx.xP;
                case CHESTPLATE: return registryPath(actual).endsWith("_chestplate"); // heuristic, see Mapping
                default: return false;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    private String registryPath(dlp item) {
        try {
            Object key = mi.h.b(item); // DefaultedRegistry<Item>.getKey(Object) -> Identifier
            return ((amo) key).a(); // getPath()
        } catch (Throwable t) {
            return "";
        }
    }

    private interface KeyAction { void run(); }

    private boolean withSelectedSlot(int slot, KeyAction action) {
        try {
            ddl inv = mc().s.gK();
            int previous = inv.g();
            inv.d(slot);
            action.run();
            keyTimer.schedule(() -> {
                try { inv.d(previous); } catch (Throwable t) { GhostClient.log("restore hotbar slot failed: " + t); }
            }, 120, TimeUnit.MILLISECONDS);
            return true;
        } catch (Throwable t) {
            GhostClient.log("VanillaBridge.withSelectedSlot() failed: " + t);
            return false;
        }
    }

    /** Presses a KeyMapping and releases it ~100ms later (two client ticks), like a real tap. */
    private void pulseKey(gfh key) {
        key.a(true);
        keyTimer.schedule(() -> {
            try { key.a(false); } catch (Throwable t) { GhostClient.log("key release failed: " + t); }
        }, 100, TimeUnit.MILLISECONDS);
    }

    private static Vec3 vec(ftm v) { return new Vec3(v.g, v.h, v.i); }
}
