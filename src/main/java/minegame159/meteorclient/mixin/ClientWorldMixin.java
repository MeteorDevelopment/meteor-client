/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.entity.EntityAddedEvent;
import minegame159.meteorclient.events.entity.EntityRemovedEvent;
import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.render.Search;
import minegame159.meteorclient.systems.modules.world.Ambience;
import minegame159.meteorclient.systems.modules.render.RenderInvisible;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.SkyProperties;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Unique
    private final SkyProperties endSky = new SkyProperties.End();
    @Unique
    private final SkyProperties customSky = new Ambience.Custom();
    @Shadow
    MinecraftClient client;

    @Inject(method = "addEntityPrivate", at = @At("TAIL"))
    private void onAddEntityPrivate(int id, Entity entity, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(EntityAddedEvent.get(entity));
    }

    @Inject(method = "finishRemovingEntity", at = @At("TAIL"))
    private void onFinishRemovingEntity(Entity entity, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(EntityRemovedEvent.get(entity));
    }

    @Inject(method = "setBlockStateWithoutNeighborUpdates", at = @At("TAIL"))
    private void onSetBlockStateWithoutNeighborUpdates(BlockPos blockPos, BlockState blockState, CallbackInfo info) {
        Search search = Modules.get().get(Search.class);
        if (search.isActive()) search.onBlockUpdate(blockPos, blockState);
    }

    /**
     * @author Walaryne
     */
    @Inject(method = "method_23777", at = @At("HEAD"), cancellable = true)
    private void onGetSkyColor(BlockPos blockPos, float tickDelta, CallbackInfoReturnable<Vec3d> info) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.changeSkyColor.get()) {
            info.setReturnValue(ambience.skyColor.get().getVec3d());
        }
    }

    /**
     * @author Walaryne
     */
    @Inject(method = "getSkyProperties", at = @At("HEAD"), cancellable = true)
    private void onGetSkyProperties(CallbackInfoReturnable<SkyProperties> info) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.enderMode.get()) {
            info.setReturnValue(ambience.enderCustomSkyColor.get() ? customSky : endSky);
        }
    }

    /**
     * @author Walaryne
     */
    @Inject(method = "getCloudsColor", at = @At("HEAD"), cancellable = true)
    private void onGetCloudsColor(float tickDelta, CallbackInfoReturnable<Vec3d> info) {
        Ambience ambience = Modules.get().get(Ambience.class);

        if (ambience.isActive() && ambience.changeCloudColor.get()) {
            info.setReturnValue(ambience.cloudColor.get().getVec3d());
        }
    }

    @Inject(at = @At("HEAD"), method = "doRandomBlockDisplayTicks", cancellable = true)
    public void doRandomBlockDisplayTicks(int xCenter, int yCenter, int i, CallbackInfo info) {
        Random random = new Random();
        boolean showBarrierParticles = this.client.interactionManager.getCurrentGameMode() == GameMode.CREATIVE && (
                this.client.player.inventory.getMainHandStack().getItem() == Items.BARRIER || 
                this.client.player.inventory.offHand.get(0).getItem() == Items.BARRIER);
        if (Modules.get().get(RenderInvisible.class).renderBarriers()) showBarrierParticles = true;

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for(int k = 0; k < 667; ++k) {
            client.world.randomBlockDisplayTick(xCenter, yCenter, i, 16, random, showBarrierParticles, mutable);
            client.world.randomBlockDisplayTick(xCenter, yCenter, i, 32, random, showBarrierParticles, mutable);
        }

        info.cancel();
    }
}
