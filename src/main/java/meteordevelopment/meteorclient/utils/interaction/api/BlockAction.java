/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.interaction.api;

import net.minecraft.util.math.BlockPos;

public interface BlockAction extends Action {
    /** The position of this action. */
    BlockPos getPos();
}
