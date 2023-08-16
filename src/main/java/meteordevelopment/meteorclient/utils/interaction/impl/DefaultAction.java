/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.interaction.impl;

import meteordevelopment.meteorclient.utils.interaction.api.Action;

public class DefaultAction implements Action {
    private final int priority;

    private State state;

    public DefaultAction(int priority, State state) {
        this.priority = priority;
        this.state = state;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
