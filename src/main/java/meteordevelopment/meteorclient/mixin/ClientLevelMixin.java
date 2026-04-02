/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Shadow
    @Nullable
    public abstract Entity getEntity(int id);

    @Inject(method = "addEntity", at = @At("TAIL"))
    private void onAddEntity(Entity entity, CallbackInfo ci) {
        if (entity != null) MeteorClient.EVENT_BUS.post(EntityAddedEvent.get(entity));
    }

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void onRemoveEntity(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci) {
        if (getEntity(entityId) != null)
            MeteorClient.EVENT_BUS.post(EntityRemovedEvent.get(getEntity(entityId)));
    }

    @ModifyArgs(method = "animateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;doAnimateTick(IIIILnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos$MutableBlockPos;)V"))
    private void doRandomBlockDisplayTicks(Args args) {
        if (Modules.get().get(NoRender.class).noBarrierInvis()) {
            args.set(5, Blocks.BARRIER);
        }
    }

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void onAddDestroyBlockEffect(BlockPos blockPos, BlockState state, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noBlockBreakParticles()) ci.cancel();
    }

    @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
    private void onAddBlockBreakingParticles(BlockPos blockPos, Direction direction, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noBlockBreakParticles()) ci.cancel();
    }
}
