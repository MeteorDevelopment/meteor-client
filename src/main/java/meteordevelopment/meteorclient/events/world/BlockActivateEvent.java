/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.events.world;

import net.minecraft.block.BlockState;

public class BlockActivateEvent {
    private static final BlockActivateEvent INSTANCE = new BlockActivateEvent();

    public BlockState blockState;

    public static BlockActivateEvent get(BlockState blockState) {
        INSTANCE.blockState = blockState;
        return INSTANCE;
    }
}
