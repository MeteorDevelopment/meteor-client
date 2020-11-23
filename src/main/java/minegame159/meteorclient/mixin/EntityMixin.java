/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.MeteorClient;
import minegame159.meteorclient.events.EventStore;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.movement.NoSlow;
import minegame159.meteorclient.modules.movement.Velocity;
import minegame159.meteorclient.modules.render.ESP;
import minegame159.meteorclient.utils.Outlines;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Redirect(method = "setVelocityClient", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setVelocity(DDD)V"))
    private void setVelocityClientEntiySetVelocityProxy(Entity entity, double x, double y, double z) {
        if ((Object) this != MinecraftClient.getInstance().player) {
            entity.setVelocity(x, y, z);
            return;
        }

        Velocity velocity = ModuleManager.INSTANCE.get(Velocity.class);
        System.out.println(entity.getVelocity());
        //System.out.println(x + ", " + y + ", " + z);
        //System.out.println((x * velocity.getHorizontal()) + ", " + (y * velocity.getVertical()) + ", " + (z * velocity.getHorizontal()));
        entity.setVelocity(entity.getVelocity().x + x * velocity.getHorizontal(), entity.getVelocity().y + y * velocity.getVertical(), entity.getVelocity().z + z * velocity.getHorizontal());
    }

    @Redirect(method = "addVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d addVelocityVec3dAddProxy(Vec3d vec3d, double x, double y, double z) {
        if ((Object) this != MinecraftClient.getInstance().player) return vec3d.add(x, y, z);

        Velocity velocity = ModuleManager.INSTANCE.get(Velocity.class);
        return vec3d.add(x * velocity.getHorizontal(), y * velocity.getVertical(), z * velocity.getHorizontal());
    }

    @Inject(method = "move", at = @At("HEAD"))
    private void onMove(MovementType type, Vec3d movement, CallbackInfo info) {
        if ((Object) this != MinecraftClient.getInstance().player) return;

        MeteorClient.EVENT_BUS.post(EventStore.playerMoveEvent(type, movement));
    }

    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> info) {
        if (Outlines.renderingOutlines) {
            info.setReturnValue(ModuleManager.INSTANCE.get(ESP.class).getColor((Entity) (Object) this).getPacked());
        }
    }

    @Redirect(method = "getVelocityMultiplier", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getBlock()Lnet/minecraft/block/Block;"))
    private Block getVelocityMultiplierGetBlockProxy(BlockState blockState) {
        if (blockState.getBlock() == Blocks.SOUL_SAND && ModuleManager.INSTANCE.get(NoSlow.class).soulSand()) return Blocks.STONE;
        return blockState.getBlock();
    }
}
