/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import meteordevelopment.meteorclient.mixininterface.IClipContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClipContext.class)
public abstract class ClipContextMixin implements IClipContext {
    @Shadow
    @Final
    @Mutable
    private Vec3 from;
    @Shadow
    @Final
    @Mutable
    private Vec3 to;
    @Shadow
    @Final
    @Mutable
    private ClipContext.Block block;
    @Shadow
    @Final
    @Mutable
    private ClipContext.Fluid fluid;
    @Shadow
    @Final
    @Mutable
    private CollisionContext collisionContext;

    @Override
    public void meteor$set(Vec3 start, Vec3 end, ClipContext.Block shapeType, ClipContext.Fluid fluidHandling, Entity entity) {
        this.from = start;
        this.to = end;
        this.block = shapeType;
        this.fluid = fluidHandling;
        this.collisionContext = CollisionContext.of(entity);
    }
}
