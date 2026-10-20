/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.compat;

import org.joml.Matrix4f;

/**
 * Version-agnostic access to Minecraft.
 *
 * <p>This interface is the contract the rest of the codebase programs against. It must never
 * reference a {@code net.minecraft.*} type: every method returns primitives, strings or other
 * version-agnostic types.</p>
 *
 * <p>Each supported Minecraft version provides its own implementation (see
 * {@code meteordevelopment.meteorclient.compat.impl}) which is selected at runtime by
 * {@link Compat}. Adding a new Minecraft version therefore only requires a new implementation
 * and a build that points the {@code meteor-compat.properties} resource at it.</p>
 */
public interface ICompat {
    /** The version of Minecraft the game is currently running. */
    McVersion version();

    /** The raw version name, e.g. {@code 26.2}. */
    String versionName();

    int getWindowWidth();

    int getWindowHeight();

    double getGuiScale();

    /** The name of the world the player is currently in, safe to use for file names. */
    String getWorldName();

    /** The in-game time formatted as {@code HH:mm}. */
    String getWorldTime();

    int getRenderDistance();

    boolean isLoading();

    /** {@code true} when the client is connected to a level with a local player. */
    boolean isInGame();

    /** Whether the vanilla HUD is currently hidden by the client. */
    boolean isHudHidden();

    void setHudHidden(boolean hidden);

    /**
     * Builds an orthographic projection for the current window, applies it to the renderer
     * and returns the matrix that was used.
     *
     * @param scaled {@code true} to divide the size by the GUI scale (used when rendering in
     *               world space), {@code false} for the raw window size
     */
    Matrix4f applyProjection(boolean scaled);

    /** Classifies the currently open screen without exposing Minecraft's screen classes. */
    ScreenKind currentScreenKind();

    /** Closes the currently open screen. Returns {@code false} when no screen was open. */
    boolean closeCurrentScreen();
}
