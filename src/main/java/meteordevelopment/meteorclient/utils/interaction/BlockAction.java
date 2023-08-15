/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.interaction;

import net.minecraft.util.math.BlockPos;

public class BlockAction implements InteractionManager.Action {
    private final int priority;
    private State state;
    private final BlockPos pos;

    public BlockAction(BlockPos pos, int priority) {
        this.pos = pos;
        this.priority = priority;

        this.state = State.Pending;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }
}
