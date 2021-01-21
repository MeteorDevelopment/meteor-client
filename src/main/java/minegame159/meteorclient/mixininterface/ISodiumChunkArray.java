package minegame159.meteorclient.mixininterface;

import baritone.utils.accessor.IChunkArray;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public interface ISodiumChunkArray extends IChunkArray {
    ObjectIterator<Long2ObjectMap.Entry<Object>> callIterator();
}
