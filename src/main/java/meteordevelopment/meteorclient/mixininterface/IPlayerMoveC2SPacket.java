/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

public interface IPlayerMoveC2SPacket {
    int meteor$getTag();

    void meteor$setTag(int tag);
}
