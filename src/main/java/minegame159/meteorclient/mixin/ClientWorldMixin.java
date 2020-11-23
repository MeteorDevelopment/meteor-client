/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.render.Search;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "addEntityPrivate", at = @At("TAIL"))
    private void onAddEntityPrivate(int id, Entity entity, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(EventStore.entityAddedEvent(entity));
    }

    @Inject(method = "finishRemovingEntity", at = @At("TAIL"))
    private void onFinishRemovingEntity(Entity entity, CallbackInfo info) {
        MeteorClient.EVENT_BUS.post(EventStore.entityRemovedEvent(entity));
    }

    @Inject(method = "setBlockStateWithoutNeighborUpdates", at = @At("TAIL"))
    private void onSetBlockStateWithoutNeighborUpdates(BlockPos blockPos, BlockState blockState, CallbackInfo info) {
        Search search = ModuleManager.INSTANCE.get(Search.class);
        if (search.isActive()) search.onBlockUpdate(blockPos, blockState);
    }
}
