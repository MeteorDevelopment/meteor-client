package me.jellysquid.mods.lithium.common.block;

public interface BlockCountingSection {
    boolean mayContainAny(TrackedBlockStatePredicate trackedBlockStatePredicate);
}
