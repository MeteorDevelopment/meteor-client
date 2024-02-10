package me.jellysquid.mods.lithium.common.world.listeners;

import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;

public interface WorldBorderListenerOnce extends WorldBorderListener {

    void onWorldBorderShapeChange(WorldBorder worldBorder);

    default void onAreaReplaced(WorldBorder border) {
        this.onWorldBorderShapeChange(border);
    }

    @Override
    default void onSizeChange(WorldBorder border, double size) {
        this.onWorldBorderShapeChange(border);
    }

    @Override
    default void onInterpolateSize(WorldBorder border, double fromSize, double toSize, long time) {
        this.onWorldBorderShapeChange(border);
    }

    @Override
    default void onCenterChanged(WorldBorder border, double centerX, double centerZ) {
        this.onWorldBorderShapeChange(border);
    }

    @Override
    default void onWarningTimeChanged(WorldBorder border, int warningTime) {

    }

    @Override
    default void onWarningBlocksChanged(WorldBorder border, int warningBlockDistance) {

    }

    @Override
    default void onDamagePerBlockChanged(WorldBorder border, double damagePerBlock) {

    }

    @Override
    default void onSafeZoneChanged(WorldBorder border, double safeZoneRadius) {

    }
}
