package eg.bridge.fabric121_11;

/**
 * Mojang-name -> Fabric intermediary-name table for Minecraft 1.21.11 under the FABRIC
 * loader. Intermediary names are stable across versions and are what the game's classes
 * are actually called at runtime once Fabric Loader remaps them - unlike vanilla, where
 * the loaded classes keep Mojang's raw obfuscated names (see the sibling vanilla121_11
 * package for that case).
 *
 * Source and method: Mojang's official 1.21.11 client mappings (for member identity) and
 * Fabric's official intermediary-1.21.11 mappings
 * (net.fabricmc:intermediary:1.21.11, from maven.fabricmc.net), cross-referenced by
 * official (obfuscated) short name - the two mapping sets describe the same obfuscated
 * jar, so Mojang's obfuscated name for a member is the lookup key into Fabric's tiny file.
 * Every member below was also confirmed, with {@code javap}, to be physically declared in
 * its class in the real 1.21.11 client.jar (not guessed from the mapping text alone).
 *
 * Important quirk of intermediary's tiny v1 format: it records an overridden interface
 * method ONCE, under whichever interface first declares it, not under every class that
 * implements it. A remapper is hierarchy-aware, so the same final name still applies to
 * every implementor (Java requires that - an override can't rename independently of its
 * interface method without breaking dispatch). So e.g. Entity.getId() is looked up under
 * its "esp" interface here, not under Entity's own obfuscated name (cgk), even though
 * {@code cgk.aA()} is a real, directly compiled method on Entity. Each row below says
 * which interface the lookup actually used when it was not the member's own declaring
 * class, so the reasoning can be checked.
 *
 * net.minecraft.client.Minecraft (gfj)                 -> net.minecraft.class_310
 *   static Minecraft getInstance()            gfj.V()  -> method_1551
 *   ClientLevel level (field)                  gfj.r    -> field_1687
 *   LocalPlayer player (field)                 gfj.s    -> field_1724
 *   Options options (field)                    gfj.k    -> field_1690
 * net.minecraft.client.multiplayer.ClientLevel (hif)    -> net.minecraft.class_638
 *   List&lt;AbstractClientPlayer&gt; players() hif.E()  -> UNRESOLVED, see FabricBridge.others()
 * net.minecraft.client.player.LocalPlayer (hnh)         -> net.minecraft.class_746
 * net.minecraft.client.player.AbstractClientPlayer (hne)-> net.minecraft.class_742
 * net.minecraft.world.entity.player.Player (ddm)        -> net.minecraft.class_1657
 *   Inventory getInventory()                   ddm.gK() -> method_31548
 *   void startFallFlying()                     ddm.gS() -> method_23669
 * net.minecraft.world.entity.LivingEntity (chl)         -> net.minecraft.class_1309
 *   float getHealth()                          chl.eZ() -> method_6032
 *   boolean isDeadOrDying()                    chl.fa()  -> method_29504
 *   boolean isFallFlying()                     chl.gj()  -> method_6128
 *   ItemStack getItemBySlot(EquipmentSlot)     chl.a(cgv)      -> method_6118
 *   void setItemSlot(EquipmentSlot,ItemStack)  chl.a(cgv,dlt)  -> method_5673
 * net.minecraft.world.entity.Entity (cgk)               -> net.minecraft.class_1297
 *   boolean onGround()                         cgk.aV()  -> method_24828
 *   Vec3 getEyePosition()                      cgk.bV()  -> method_33571
 *   Vec3 getDeltaMovement()                    cgk.dN()  -> method_18798
 *   float getYRot()                            cgk.ec()  -> method_36454
 *   float getXRot()                            cgk.ee()  -> method_36455
 *   void setYRot(float)                        cgk.v(F)  -> method_36456
 *   void setXRot(float)                        cgk.w(F)  -> method_36457
 *   int getId()                       cgk.aA() via "esp"  -> method_5628
 *   String getScoreboardName()        cgk.da() via "fuq"  -> method_5820
 *   AABB getBoundingBox()             cgk.dj() via "esp"  -> method_5829
 * net.minecraft.world.phys.Vec3 (ftm)                   -> net.minecraft.class_243
 *   double x,y,z (fields)              g,h,i  -> field_1352, field_1351, field_1350
 * net.minecraft.world.phys.AABB (fth)                   -> net.minecraft.class_238
 *   double minX,minY,minZ,maxX,maxY,maxZ (fields) a,b,c,d,e,f
 *     -> field_1323, field_1322, field_1321, field_1320, field_1325, field_1324
 * net.minecraft.world.entity.player.Inventory (ddl)     -> net.minecraft.class_1661
 *   int getSelectedSlot()                       ddl.g()  -> method_67532
 *   void setSelectedSlot(int)                   ddl.d(I) -> method_61496
 *   int getContainerSize()              ddl.b() via "ccv" -> method_5439
 *   ItemStack getItem(int)             ddl.a(I) via "ccv" -> method_5438
 * net.minecraft.world.entity.EquipmentSlot (cgv)        -> net.minecraft.class_1304
 *   MAINHAND,OFFHAND,FEET,LEGS,CHEST,HEAD,BODY,SADDLE (a..h fields)
 *     -> field_6173, field_6171, field_6166, field_6172, field_6174, field_6169,
 *        field_48824, field_55946
 * net.minecraft.world.item.ItemStack (dlt)              -> net.minecraft.class_1799
 *   boolean isEmpty()                           dlt.f()  -> method_7960
 *   Item getItem()                              dlt.h()  -> method_7909
 * net.minecraft.world.item.Item (dlp)                   -> net.minecraft.class_1792
 * net.minecraft.world.item.Items (dlx)                  -> net.minecraft.class_1802
 *   Item ELYTRA (field pq)                               -> field_8833
 *   Item FIREWORK_ROCKET (field wG)                      -> field_8639
 *   Item TOTEM_OF_UNDYING (field xP)                     -> field_8288
 * net.minecraft.core.registries.BuiltInRegistries (mi)  -> net.minecraft.class_7923
 *   DefaultedRegistry&lt;Item&gt; ITEM (field h)          -> field_41178
 * net.minecraft.core.DefaultedRegistry (iy)             -> net.minecraft.class_7922
 *   Identifier getKey(Object) iy.b(Object) via "jq" (Registry) -> method_10221
 * net.minecraft.resources.Identifier (amo)              -> net.minecraft.class_2960
 *   String getPath()                            amo.a()  -> method_12832
 * net.minecraft.client.Options (gfo)                    -> net.minecraft.class_315
 *   KeyMapping keyJump (field w)                         -> field_1903
 *   KeyMapping keySwapOffhand (field A)                  -> field_1831
 *   KeyMapping keyUse (field C)                          -> field_1904
 *   KeyMapping keyAttack (field D)                       -> field_1886
 * net.minecraft.client.KeyMapping (gfh)                 -> net.minecraft.class_304
 *   boolean isDown()                            gfh.f()  -> method_1434
 *   void setDown(boolean)                       gfh.a(Z) -> method_23481
 */
final class Mapping {
    private Mapping() {}
}
