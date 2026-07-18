/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.baritoneflow;

/**
 * The kind of Baritone task a {@link FlowNode} represents.
 */
public enum FlowNodeType {
    Start,
    Goto,
    Mine,
    Follow,
    Wait,
    Pause,
    Resume,
    Stop,
    Command
}
