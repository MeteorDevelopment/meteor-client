/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.utils.misc;

public interface ICopyable<T extends ICopyable<T>> {
    void set(T value);

    T copy();
}
