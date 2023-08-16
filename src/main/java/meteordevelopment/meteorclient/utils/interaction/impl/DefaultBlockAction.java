/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.interaction.impl;

import meteordevelopment.meteorclient.utils.interaction.api.BlockAction;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import net.minecraft.util.math.BlockPos;

public class DefaultBlockAction extends DefaultAction implements BlockAction {
    private final BlockPos pos;
    private final FindItemResult item;
    public boolean rotated = false;

    public DefaultBlockAction(BlockPos pos, FindItemResult item, int priority, State state) {
        super(priority, state);

        this.pos = pos.toImmutable();
        this.item = item;
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }
}
