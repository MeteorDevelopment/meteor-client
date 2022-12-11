/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor.api.commands;

public interface CommandManager extends Iterable<Command> {
    /** Adds the command. If there already is a command with the same name it is overwritten. */
    void add(Command command);
}
