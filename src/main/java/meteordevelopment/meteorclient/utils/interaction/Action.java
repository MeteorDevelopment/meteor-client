/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.interaction;

public interface Action {
    enum State {
        Pending,
        Finished,
        Cancelled
    }

    /** The priority of this action. */
    int getPriority();

    /** The state the action is currently in. */
    State getState();
}
