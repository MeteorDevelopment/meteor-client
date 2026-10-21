/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mc;

/**
 * The mixin targets that differ between the supported Minecraft versions.
 *
 * <p>Mixin annotation values have to be compile time constants, so the version specific names
 * live here and the shared mixins in {@code src/main/java} refer to them. See COMPAT.md.
 */
public final class MixinTargets {
    private MixinTargets() {}

    public static final String ITEM_ACTIVATION_TARGET = "net.minecraft.client.renderer.GameRenderer";
    public static final String HIT_RESULT_METHOD = "getManyEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;FLnet/minecraft/world/level/ClipContext$Block;Z)Ljava/util/Collection;";
    public static final String SWING_METHOD = "swing(Lnet/minecraft/world/InteractionHand;)V";
    public static final String SKY_METHOD = "addSkyPass";
    public static final String INPUT_METHOD = "tick";
    public static final String FOIL_SUBMIT_METHOD = "prepareFoilSubmit";
    public static final String SWING_DURATION_METHOD = "getCurrentSwingDuration";
    public static final String SHADER_MANAGER_APPLY = "apply(Lnet/minecraft/client/renderer/ShaderManager$Configs;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V";
    public static final String CAMERA_FLUID_METHOD = "getFluidInCamera";
    public static final String REGISTRY_BOOTSTRAP_TARGET = "net.minecraft.core.registries.BuiltInRegistries";
    public static final String DOWNLOAD_QUEUE_TARGET = "net.minecraft.server.packs.DownloadQueue";
    public static final String REMOVE_ENTITY_IDS_INVOKE = "Lnet/minecraft/network/protocol/game/ClientboundRemoveEntitiesPacket;getEntityIds()Lit/unimi/dsi/fastutil/ints/IntList;";
}
