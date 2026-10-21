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

    public static final String ITEM_ACTIVATION_TARGET = "net.minecraft.client.player.LocalPlayer";
    public static final String SKY_METHOD = "shouldRenderSky";
    public static final String INPUT_METHOD = "sendChanges";
    public static final String FOIL_SUBMIT_METHOD = "prepareMainSubmit";
    public static final String SWING_DURATION_METHOD = "getModifiedSwingDuration";
    public static final String SHADER_MANAGER_APPLY = "apply(Lcom/mojang/blaze3d/pipeline/PipelineBuilder;Lnet/minecraft/client/renderer/ShaderManager$PendingResults;)V";
    public static final String CAMERA_FLUID_METHOD = "getFogType";
    public static final String REGISTRY_BOOTSTRAP_TARGET = "net.minecraft.core.registries.BuiltinRegistryBuilder";
    public static final String DOWNLOAD_QUEUE_TARGET = "net.minecraft.client.resources.server.DownloadQueue";
    public static final String REMOVE_ENTITY_IDS_INVOKE = "Lnet/minecraft/network/protocol/game/ClientboundRemoveEntitiesPacket;entityIds()Lit/unimi/dsi/fastutil/ints/IntList;";
}
