/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor.impl.mixin;

import org.meteordev.meteor.api.MeteorAPI;
import org.meteordev.meteor.impl.MeteorAPIImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = MeteorAPI.class, remap = false)
public interface MeteorAPIMixin {
    /**
     * @author Meteor Development
     * @reason Implement getInstance() interface method
     */
    @Overwrite
    static MeteorAPI getInstance() {
        return MeteorAPIImpl.INSTANCE;
    }
}
