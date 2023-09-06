/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import java.util.Collection;

public enum FilterMode {
    Whitelist,
    Blacklist,
    None,
    All;

    public <T> boolean test(Collection<T> list, T element) {
        if (this == Whitelist && list.contains(element)) return true;
        else if (this == Blacklist && !list.contains(element)) return true;

        return this == None;
    }

    public boolean isWildCard() {
        return this == None || this == All;
    }
}
