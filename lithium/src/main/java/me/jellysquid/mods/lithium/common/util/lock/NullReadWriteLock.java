package me.jellysquid.mods.lithium.common.util.lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * A ReadWriteLock which doesn't do anything.
 */
public class NullReadWriteLock implements ReadWriteLock {
    private final NullLock lock = new NullLock();

    @Override
    public Lock readLock() {
        return this.lock;
    }

    @Override
    public Lock writeLock() {
        return this.lock;
    }
}
