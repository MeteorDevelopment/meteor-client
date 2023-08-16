/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.interaction;


import net.minecraft.entity.Entity;

public interface EntityAction extends Action {
    /** The entity this action targets. */
    Entity getEntity();
}
