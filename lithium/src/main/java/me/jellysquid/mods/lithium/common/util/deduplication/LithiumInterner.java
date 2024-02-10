package me.jellysquid.mods.lithium.common.util.deduplication;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class LithiumInterner<T> {
    private final ObjectOpenHashSet<T> canonicalStorage = new ObjectOpenHashSet<>();

    public T getCanonical(T value) {
        return this.canonicalStorage.addOrGet(value);
    }

    public void deleteCanonical(T value) {
        this.canonicalStorage.remove(value);
    }
}
