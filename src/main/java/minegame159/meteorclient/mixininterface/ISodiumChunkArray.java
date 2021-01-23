/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.mixininterface;

import baritone.utils.accessor.IChunkArray;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public interface ISodiumChunkArray extends IChunkArray {
    ObjectIterator<Long2ObjectMap.Entry<Object>> callIterator();
}
