/*
 *
 *  * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 *  * Copyright (c) 2021 Meteor Development.
 *
 */

package minegame159.meteorclient.mixin;

import baritone.api.utils.Rotation;
import baritone.behavior.LookBehavior;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = LookBehavior.class, remap = false)
public interface LookBehaviourAccessor {
    @Accessor("target")
    Rotation getTarget();
}
