/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

public enum ListMode {
    Whitelist,
    Blacklist;

    public boolean allows(boolean contains) {
        return switch (this) {
            case Whitelist -> contains;
            case Blacklist -> !contains;
        };
    }
}
