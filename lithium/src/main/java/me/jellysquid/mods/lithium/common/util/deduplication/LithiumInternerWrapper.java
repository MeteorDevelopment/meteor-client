package me.jellysquid.mods.lithium.common.util.deduplication;

public interface LithiumInternerWrapper<T> {

    T getCanonical(T value);

    void deleteCanonical(T value);
}
