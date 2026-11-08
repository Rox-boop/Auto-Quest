
/**
 * Obfuscated-name -> Mojang-name table for Minecraft 1.21.11, VANILLA launcher only
 * (official Mojang client, launched unmodified: no Fabric/Forge/Lunar/Badlion remapping).
 *
 * Source: Mojang's own official client mappings for 1.21.11
 * (sha1 031a68bebf55d824f66d6573d8c752f0e1bf232a, from the public version manifest),
 * cross-checked against the actual obfuscated client.jar
 * (sha1 ba2df812c2d12e0219c489c4cd9a5e1f0760f5bd) with {@code javap}: every member this
 * bridge touches was confirmed present, public, and with the listed signature before use.
 *
 * This is a documentation-only class (no redistribution of the mapping file itself,
 * per Mojang's mapping license - "you may not redistribute the mappings complete and
 * unmodified"): it lists just the subset of names {@link VanillaBridge} relies on.
 *
 * If Mojang ships an obfuscation remap for a later 1.21.11 patch, every name below can
 * silently stop matching. {@link VanillaBridge#ready()} checks class identity before use;
 * it does not, and cannot, check that a matching class still has the right member names -
 * a bytecode mismatch there throws NoSuchMethodError/NoSuchFieldError, which the ticker in
 * {@code GhostClient} catches and disables the offending module.
 *
 * Mojang name                                        -> obfuscated runtime name
 * ----------------------------------------------------------------------------
 * net.minecraft.client.Minecraft                      -> gfj
 *   static Minecraft getInstance()                    -> V()
 *   ClientLevel level (field)                          -> r
 *   LocalPlayer player (field)                         -> s
 *   Options options (field)                            -> k
 * net.minecraft.client.multiplayer.ClientLevel         -> hif
 *   List&lt;AbstractClientPlayer&gt; players()          -> E()
 * net.minecraft.client.player.LocalPlayer              -> hnh   (extends hne)
 * net.minecraft.client.player.AbstractClientPlayer     -> hne   (extends ddm)
 * net.minecraft.world.entity.player.Player             -> ddm   (extends chl)
 *   Inventory getInventory()                           -> gK()
 *   void startFallFlying()                             -> gS()
 * net.minecraft.world.entity.LivingEntity              -> chl   (extends cgk)
 *   float getHealth()                                  -> eZ()
 *   boolean isDeadOrDying()                             -> fa()
 *   boolean isFallFlying()                              -> gj()
 *   ItemStack getItemBySlot(EquipmentSlot)              -> a(cgv)
 *   void setItemSlot(EquipmentSlot, ItemStack)          -> a(cgv, dlt)
 * net.minecraft.world.entity.Entity                    -> cgk
 *   int getId()                                        -> aA()
 *   String getScoreboardName()                         -> da()
 *   boolean onGround()                                 -> aV()
 *   Vec3 position()                                    -> dI()
 *   Vec3 getEyePosition()                              -> bV()
 *   Vec3 getDeltaMovement()                             -> dN()
 *   AABB getBoundingBox()                               -> dj()
 *   float getYRot()                                    -> ec()
 *   void setYRot(float)                                -> v(float)
 *   float getXRot()                                    -> ee()
 *   void setXRot(float)                                -> w(float)
 * net.minecraft.world.phys.Vec3                        -> ftm
 *   double x,y,z (fields)                              -> g,h,i
 * net.minecraft.world.phys.AABB                        -> fth
 *   double minX,minY,minZ,maxX,maxY,maxZ (fields)       -> a,b,c,d,e,f
 * net.minecraft.world.entity.player.Inventory          -> ddl
 *   int getSelectedSlot()                               -> g()
 *   void setSelectedSlot(int)                           -> d(int)
 *   int getContainerSize()                              -> b()
 *   ItemStack getItem(int)                              -> a(int)
 * net.minecraft.world.entity.EquipmentSlot              -> cgv
 *   MAINHAND, OFFHAND, FEET, LEGS, CHEST, HEAD, BODY, SADDLE -> a,b,c,d,e,f,g,h
 * net.minecraft.world.item.ItemStack                    -> dlt
 *   boolean isEmpty()                                   -> f()
 *   Item getItem()                                      -> h()
 * net.minecraft.world.item.Item                         -> dlp
 * net.minecraft.world.item.Items                        -> dlx
 *   Item ELYTRA                                         -> pq
 *   Item FIREWORK_ROCKET                                -> wG
 *   Item TOTEM_OF_UNDYING                                -> xP
 * net.minecraft.core.registries.BuiltInRegistries        -> mi
 *   DefaultedRegistry&lt;Item&gt; ITEM (field)             -> h
 * net.minecraft.core.DefaultedRegistry                   -> iy
 *   Identifier getKey(Object)                           -> b(Object)
 * net.minecraft.resources.Identifier                     -> amo
 *   String getPath()                                    -> a()
 * net.minecraft.client.Options                           -> gfo
 *   KeyMapping keyJump (field)                          -> w
 *   KeyMapping keySwapOffhand (field)                   -> A
 *   KeyMapping keyUse (field)                           -> C
 *   KeyMapping keyAttack (field)                        -> D
 * net.minecraft.client.KeyMapping                        -> gfh
 *   boolean isDown()                                    -> f()
 *   void setDown(boolean)                               -> a(boolean)
 */
final class Mapping {
    private Mapping() {}
}
