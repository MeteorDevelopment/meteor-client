/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;

import java.util.List;

public enum FilterMode {
    Whitelist,
    Blacklist,
    None,
    All;

    public <T> boolean test(List<T> list, T element) {
        if (this == Whitelist && list.contains(element)) return true;
        else if (this == Blacklist && !list.contains(element)) return true;

        return this == None;
    }

    public boolean test(Object2BooleanMap<?> map, Object key) {
        if (this == Whitelist && map.getBoolean(key)) return true;
        else if (this == Blacklist && !map.getBoolean(key)) return true;

        return this == None;
    }

    public boolean isWildCard() {
        return this == None || this == All;
    }
}
