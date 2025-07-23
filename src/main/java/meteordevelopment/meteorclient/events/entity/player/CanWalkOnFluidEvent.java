/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.entity.player;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;

/**
 * As of writing, using any method that gets the players pose within this event will cause a crash.
 * <p>
 * Getting the pose calls {@link meteordevelopment.meteorclient.mixin.EntityMixin#modifyGetPose(net.minecraft.entity.EntityPose)},
 * which calls {@link PlayerEntity#canChangeIntoPose(net.minecraft.entity.EntityPose)}, which
 * calculates whether there is enough space to fit your bounding box if you change into that pose. This method ends up
 * calling {@link LivingEntity#canWalkOnFluid(net.minecraft.fluid.FluidState)}, causing this event to fire
 * again and leading to a stack overflow crash. Introduced in
 * <a href="https://github.com/MeteorDevelopment/meteor-client/pull/5449">this pull request</a>
 */
public class CanWalkOnFluidEvent {
    private static final CanWalkOnFluidEvent INSTANCE = new CanWalkOnFluidEvent();

    public FluidState fluidState;
    public boolean walkOnFluid;

    public static CanWalkOnFluidEvent get(FluidState fluid) {
        INSTANCE.fluidState = fluid;
        INSTANCE.walkOnFluid = false;
        return INSTANCE;
    }
}
