/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.compat;

/**
 * Version-agnostic classification of the screen the client is currently showing.
 *
 * <p>Minecraft's screen classes get renamed and reshuffled between versions, so code that
 * only needs to know <em>which</em> screen is open should match on this enum instead of
 * {@code instanceof} checks against {@code net.minecraft} screen classes. That keeps the
 * mapping knowledge inside the {@link ICompat} implementation.</p>
 *
 * <p>Additional categories can be added here as more version-fragile call sites are migrated;
 * callers only ever see this enum.</p>
 */
public enum ScreenKind {
    /** No screen is open. */
    NONE,
    /** The main menu. */
    TITLE,
    /** The multiplayer server list. */
    MULTIPLAYER,
    /** The world selection screen. */
    WORLD_SELECTION,
    /** Any other screen, including screens opened by Meteor itself. */
    OTHER
}
