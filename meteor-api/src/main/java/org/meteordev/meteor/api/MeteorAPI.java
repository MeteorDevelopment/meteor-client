/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor.api;

import org.jetbrains.annotations.NotNull;
import org.meteordev.meteor.api.commands.CommandManager;

public interface MeteorAPI {
    @SuppressWarnings("DataFlowIssue")
    @NotNull
    static MeteorAPI getInstance() {
        return null;
    }

    /** @return the version of the API. */
    String getVersion();

    /** @return the command manager. */
    CommandManager getCommands();
}
