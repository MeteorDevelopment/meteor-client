/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package org.meteordev.meteor.api.addons;

public interface AddonManager extends Iterable<Addon> {
    /** Adds the addon. If there already is an addon with the same name it is overwritten. */
    void add(Addon addon);

    /** @return the addon with the provided id or null. */
    Addon get(String id);

    /** @throws IllegalStateException if the provided addon has not yet been added to the manager. */
    default void checkValid(String id) {
        if (get(id) == null) {
            throw new IllegalStateException("Tried to use an addon with an id '" + id + "' but no such addon exists.");
        }
    }

    /** @throws IllegalStateException if the provided addon has not yet been added to the manager. */
    default void checkValid(Addon addon) {
        checkValid(addon.getId());
    }
}
