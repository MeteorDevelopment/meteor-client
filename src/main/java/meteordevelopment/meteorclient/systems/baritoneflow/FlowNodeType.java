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
    Notify,
    Leave,
    Reconnect,
    OnPlayerNearAppear,
    OnHunger,
    OnHostileMobsNear,
    OnLowHealth,
    OnInventoryFull,
    OnDisconnect;

    public boolean isTrigger() {
        return category() == Category.Trigger;
    }

    public Category category() {
        return switch (this) {
            case OnPlayerNearAppear, OnHunger, OnHostileMobsNear, OnLowHealth, OnInventoryFull, OnDisconnect -> Category.Trigger;
            case Module -> Category.Module;
            case Notify, Leave, Reconnect -> Category.Client;
            default -> Category.Baritone;
        };
    }

    /** Nicer display text than {@link #toString()} for node types whose name doesn't read well on its own. */
    public String label() {
        return switch (this) {
            case OnPlayerNearAppear -> "Player Near";
            case OnHunger -> "On Hunger";
            case OnHostileMobsNear -> "Hostile Mobs Near";
            case OnLowHealth -> "On Low Health";
            case OnInventoryFull -> "Inventory Full";
            case OnDisconnect -> "On Disconnect";
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
