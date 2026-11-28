/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.utils;

public interface CharFilter {
    boolean filter(String text, char c);

    default boolean filter(String text, int i) {
        return filter(text, (char) i);
    }
}
