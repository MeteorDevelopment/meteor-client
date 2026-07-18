/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.baritoneflow;

/**
 * The kind of step a {@link FlowNode} represents: a Baritone task, a Meteor module action, a
 * client-level action, or an event trigger that starts a flow on its own.
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
    Command,
    Module,
    Leave,
    Reconnect,
    OnPlayerNearAppear;

    public Category category() {
        return switch (this) {
            case OnPlayerNearAppear -> Category.Trigger;
            case Module -> Category.Module;
            case Leave, Reconnect -> Category.Client;
            default -> Category.Baritone;
        };
    }

    /** Nicer display text than {@link #toString()} for node types whose name doesn't read well on its own. */
    public String label() {
        return switch (this) {
            case OnPlayerNearAppear -> "Player Near";
            default -> toString();
        };
    }

    public enum Category {
        Trigger,
        Baritone,
        Module,
        Client
    }
}
