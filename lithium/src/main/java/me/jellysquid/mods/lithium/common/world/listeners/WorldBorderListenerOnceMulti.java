package me.jellysquid.mods.lithium.common.world.listeners;

import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.border.WorldBorderListener;

import java.util.WeakHashMap;

public class WorldBorderListenerOnceMulti implements WorldBorderListener {

    private final WeakHashMap<WorldBorderListenerOnce, Object> delegate;

    public WorldBorderListenerOnceMulti() {
        this.delegate = new WeakHashMap<>();
    }

    public void add(WorldBorderListenerOnce listener) {
        this.delegate.put(listener, null);
    }

    public void onAreaReplaced(WorldBorder border) {
        for (WorldBorderListenerOnce listener : this.delegate.keySet()) {
            listener.onAreaReplaced(border);
        }
        this.delegate.clear();
    }

    @Override
    public void onSizeChange(WorldBorder border, double size) {
        for (WorldBorderListenerOnce listener : this.delegate.keySet()) {
            listener.onSizeChange(border, size);
        }
        this.delegate.clear();
    }

    @Override
    public void onInterpolateSize(WorldBorder border, double fromSize, double toSize, long time) {
        for (WorldBorderListenerOnce listener : this.delegate.keySet()) {
            listener.onInterpolateSize(border, fromSize, toSize, time);
        }
        this.delegate.clear();
    }

    @Override
    public void onCenterChanged(WorldBorder border, double centerX, double centerZ) {
        for (WorldBorderListenerOnce listener : this.delegate.keySet()) {
            listener.onCenterChanged(border, centerX, centerZ);
        }
        this.delegate.clear();
    }

    @Override
    public void onWarningTimeChanged(WorldBorder border, int warningTime) {
        for (WorldBorderListenerOnce listener : this.delegate.keySet()) {
            listener.onWarningTimeChanged(border, warningTime);
        }
        this.delegate.clear();
    }

    @Override
    public void onWarningBlocksChanged(WorldBorder border, int warningBlockDistance) {
        for (WorldBorderListenerOnce listener : this.delegate.keySet()) {
            listener.onWarningBlocksChanged(border, warningBlockDistance);
        }
        this.delegate.clear();
    }

    @Override
    public void onDamagePerBlockChanged(WorldBorder border, double damagePerBlock) {
        for (WorldBorderListenerOnce listener : this.delegate.keySet()) {
            listener.onDamagePerBlockChanged(border, damagePerBlock);
        }
        this.delegate.clear();
    }

    @Override
    public void onSafeZoneChanged(WorldBorder border, double safeZoneRadius) {
        for (WorldBorderListenerOnce listener : this.delegate.keySet()) {
            listener.onSafeZoneChanged(border, safeZoneRadius);
        }
        this.delegate.clear();
    }
}
