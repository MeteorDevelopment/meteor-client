/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.compat.impl;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.compat.ICompat;
import meteordevelopment.meteorclient.compat.McVersion;
import meteordevelopment.meteorclient.compat.ScreenKind;
import meteordevelopment.meteorclient.mixin.ClientPacketListenerAccessor;
import meteordevelopment.meteorclient.mixin.MinecraftAccessor;
import meteordevelopment.meteorclient.mixin.MinecraftServerAccessor;
import meteordevelopment.meteorclient.mixin.ReloadStateAccessor;
import meteordevelopment.meteorclient.mixin.ResourceLoadStateTrackerAccessor;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.SharedConstants;
import net.minecraft.client.ResourceLoadStateTracker;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.joml.Matrix4f;

import java.io.File;

/**
 * Compatibility implementation for Minecraft 26.2.
 *
 * <p>This class is intentionally the only place that knows how the 26.2 mappings expose these
 * APIs. When a new Minecraft version is supported a sibling class is added and the build points
 * {@code /meteor-compat.properties} at it.</p>
 */
public final class Mc263Compat implements ICompat {
    private static final ProjectionMatrixBuffer matrixBuffer = new ProjectionMatrixBuffer("meteor-projection-matrix");

    @Override
    public McVersion version() {
        return McVersion.parse(versionName());
    }

    @Override
    public String versionName() {
        return SharedConstants.getCurrentVersion().name();
    }

    @Override
    public int getWindowWidth() {
        return MeteorClient.mc.getWindow().getWidth();
    }

    @Override
    public int getWindowHeight() {
        return MeteorClient.mc.getWindow().getHeight();
    }

    @Override
    public double getGuiScale() {
        return MeteorClient.mc.getWindow().getGuiScale();
    }

    @Override
    public String getWorldName() {
        // Singleplayer
        if (MeteorClient.mc.isLocalServer()) {
            if (MeteorClient.mc.level == null) return "";
            if (MeteorClient.mc.getSingleplayerServer() == null) return "FAILED_BECAUSE_LEFT_WORLD";

            File folder = ((MinecraftServerAccessor) MeteorClient.mc.getSingleplayerServer())
                .meteor$getStorageSource()
                .getDimensionPath(MeteorClient.mc.level.dimension())
                .toFile();

            if (folder.toPath().relativize(MeteorClient.mc.gameDirectory.toPath()).getNameCount() != 2) {
                folder = folder.getParentFile();
            }

            return folder.getName();
        }

        // Multiplayer
        if (MeteorClient.mc.getCurrentServer() != null) {
            return MeteorClient.mc.getCurrentServer().isRealm() ? "realms" : MeteorClient.mc.getCurrentServer().ip;
        }

        return "";
    }

    @Override
    public String getWorldTime() {
        if (MeteorClient.mc.level == null) return "00:00";

        int ticks = (int) (MeteorClient.mc.level.getGameTime() % 24000);
        ticks += 6000;
        if (ticks > 24000) ticks -= 24000;

        return String.format("%02d:%02d", ticks / 1000, (int) (ticks % 1000 / 1000.0 * 60));
    }

    @Override
    public int getRenderDistance() {
        return Math.max(
            MeteorClient.mc.options.renderDistance().get(),
            ((ClientPacketListenerAccessor) MeteorClient.mc.getConnection()).meteor$getServerChunkRadius()
        );
    }

    @Override
    public boolean isLoading() {
        ResourceLoadStateTracker.ReloadState state = ((ResourceLoadStateTrackerAccessor) ((MinecraftAccessor) MeteorClient.mc)
            .meteor$getReloadStateTracker())
            .meteor$getReloadState();

        return state == null || !((ReloadStateAccessor) state).meteor$isFinished();
    }

    @Override
    public boolean isInGame() {
        return MeteorClient.mc != null && MeteorClient.mc.level != null && MeteorClient.mc.player != null;
    }

    @Override
    public boolean isHudHidden() {
        return MeteorClient.mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden;
    }

    @Override
    public void setHudHidden(boolean hidden) {
        MeteorClient.mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden = hidden;
    }

    @Override
    public Matrix4f applyProjection(boolean scaled) {
        Window window = MeteorClient.mc.getWindow();

        float width = scaled ? (float) (window.getWidth() / window.getGuiScale()) : window.getWidth();
        float height = scaled ? (float) (window.getHeight() / window.getGuiScale()) : window.getHeight();

        Projection proj = new Projection();
        proj.setupOrtho(-10, 100, width, height, true);

        Matrix4f matrix = proj.getMatrix(new Matrix4f());
        RenderSystem.setProjectionMatrix(matrixBuffer.getBuffer(matrix), scaled ? ProjectionType.PERSPECTIVE : ProjectionType.ORTHOGRAPHIC);

        return matrix;
    }

    @Override
    public ScreenKind currentScreenKind() {
        Screen screen = MeteorClient.mc.gui.screen();
        if (screen == null) return ScreenKind.NONE;
        if (screen instanceof TitleScreen) return ScreenKind.TITLE;
        if (screen instanceof JoinMultiplayerScreen) return ScreenKind.MULTIPLAYER;
        if (screen instanceof SelectWorldScreen) return ScreenKind.WORLD_SELECTION;

        return ScreenKind.OTHER;
    }

    @Override
    public boolean closeCurrentScreen() {
        Screen screen = MeteorClient.mc.gui.screen();
        if (screen == null) return false;

        screen.onClose();
        return true;
    }
}
