/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import meteordevelopment.meteorclient.systems.modules.world.Collisions;
import meteordevelopment.meteorclient.systems.modules.player.GhostHand;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.block.AbstractBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.CollisionView;
import net.minecraft.block.ShapeContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class AbstractBlockStateMixin {
    @Inject(method = "getModelOffset", at = @At("HEAD"), cancellable = true)
    private void modifyPos(BlockPos pos, CallbackInfoReturnable<Vec3d> info) {
        if (Modules.get() == null) return;

        if (Modules.get().get(NoRender.class).noTextureRotations()) info.setReturnValue(Vec3d.ZERO);
    }

    @Inject(at = @At("HEAD"), method = "getOutlineShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;", cancellable = true)
	private void onGetOutlineShape(BlockView view, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir)
	{
	  if (Modules.get() == null ) return;
	  if(context == ShapeContext.absent()) return;

      if (Modules.get().get(GhostHand.class).inBlockList(view.getBlockState(pos).getBlock()))
	  cir.setReturnValue(VoxelShapes.empty());
	}
	
	@Inject(at = @At("HEAD"), method = "getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;", cancellable = true)
	private void onGetCollisionShape(BlockView world, BlockPos pos, ShapeContext context, CallbackInfoReturnable<VoxelShape> cir)
	{
		if (Modules.get() == null) return;
	    if(Modules.get().get(Collisions.class).full(world.getBlockState(pos).getBlock()))
            cir.setReturnValue(VoxelShapes.fullCube());
		if(Modules.get().get(Collisions.class).emp(world.getBlockState(pos).getBlock()))
            cir.setReturnValue(VoxelShapes.empty());	
	}
}
