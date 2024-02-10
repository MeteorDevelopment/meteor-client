package me.jellysquid.mods.lithium.common.util.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * A Lock which doesn't do anything.
 */
public class NullLock implements Lock {
    @Override
    public void lock() {

    }

    @Override
    public void lockInterruptibly() {

    }

    @Override
    public boolean tryLock() {
        return true;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        return true;
    }

    @Override
    public void unlock() {

    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }
}
