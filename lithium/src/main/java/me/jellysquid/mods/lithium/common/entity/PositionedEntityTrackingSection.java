package me.jellysquid.mods.lithium.common.entity;

public interface PositionedEntityTrackingSection {
    void setPos(long chunkSectionPos);

    long getPos();
}
