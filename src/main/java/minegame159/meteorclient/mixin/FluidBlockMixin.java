/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.modules.movement.Jesus;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FluidBlock.class)
public abstract class FluidBlockMixin extends Block implements FluidDrainable {
    public FluidBlockMixin(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        if (ModuleManager.INSTANCE != null && ModuleManager.INSTANCE.get(Jesus.class).shouldBeSolid()) return VoxelShapes.fullCube();
        return super.getCollisionShape(state, view, pos, context);
    }
}
