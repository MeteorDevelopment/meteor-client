/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor.api.addons;

/** Base interface to create a Meteor addon. */
public interface Addon {
    /** @return the id of the addon, should not contain any whitespace. */
    String getId();

    /** @return the human-readable name of the addon. */
    String getName();

    /** @return all the authors of the addon. */
    String[] getAuthors();
}
