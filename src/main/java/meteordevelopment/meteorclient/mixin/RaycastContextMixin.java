/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IRaycastContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RaycastContext.class)
public class RaycastContextMixin implements IRaycastContext {
    @Shadow @Final @Mutable private Vec3d start;
    @Shadow @Final @Mutable private Vec3d end;
    @Shadow @Final @Mutable private RaycastContext.ShapeType shapeType;
    @Shadow @Final @Mutable private RaycastContext.FluidHandling fluid;
    @Shadow @Final @Mutable private ShapeContext entityPosition;

    @Override
    public void set(Vec3d start, Vec3d end, RaycastContext.ShapeType shapeType, RaycastContext.FluidHandling fluidHandling, Entity entity) {
        this.start = start;
        this.end = end;
        this.shapeType = shapeType;
        this.fluid = fluidHandling;
        this.entityPosition = ShapeContext.of(entity);
    }
}
