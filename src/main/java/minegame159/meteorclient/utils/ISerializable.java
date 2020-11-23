/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.utils;

import net.minecraft.nbt.CompoundTag;

public interface ISerializable<T> {
    CompoundTag toTag();

    T fromTag(CompoundTag tag);
}
