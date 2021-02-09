/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixininterface;

public interface IChatHudLine<T> {
    void setText(T text);

    void setTimestamp(int timestamp);

    void setId(int id);
}
