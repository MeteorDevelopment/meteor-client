package me.jellysquid.mods.lithium.common.util.collections;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/**
 * An extension for {@link Long2ObjectOpenHashMap} which allows callbacks to be installed for when an item is added to
 * or removed from the map.
 */
public class ListeningLong2ObjectOpenHashMap<V> extends Long2ObjectOpenHashMap<V> {
    private final Callback<V> addCallback, removeCallback;

    public ListeningLong2ObjectOpenHashMap(Callback<V> addCallback, Callback<V> removeCallback) {
        this.addCallback = addCallback;
        this.removeCallback = removeCallback;
    }

    @Override
    public V put(long k, V v) {
        V ret = super.put(k, v);

        if (ret != v) {
            if (ret != null) {
                this.removeCallback.apply(k, v);
            }

            this.addCallback.apply(k, v);
        }

        return ret;
    }

    @Override
    public V remove(long k) {
        V ret = super.remove(k);

        if (ret != null) {
            this.removeCallback.apply(k, ret);
        }

        return ret;
    }

    public interface Callback<V> {
        void apply(long key, V value);
    }
}
