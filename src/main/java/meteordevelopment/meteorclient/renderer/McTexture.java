/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

/**
 * A texture that lives in Minecraft's texture manager.
 *
 * <p>The concrete Minecraft type differs between the supported versions, so the shared code only
 * ever sees this wrapper; the per-version shim ({@code meteordevelopment.meteorclient.mc.Vanilla})
 * knows how to unwrap it.
 */
public final class McTexture {
    private final Object texture;

    public McTexture(Object texture) {
        this.texture = texture;
    }

    /** The underlying Minecraft texture. */
    public Object handle() {
        return texture;
    }
}
