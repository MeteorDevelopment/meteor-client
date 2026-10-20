/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixininterface;

public interface IGpuDevice {
    /**
     * Currently there can only be a single scissor pushed at once.
     */
    void meteor$pushScissor(int x, int y, int width, int height);

    void meteor$popScissor();

    /**
     * This is an *INTERNAL* method, it shouldn't be called.
     *
     * <p>The backend is passed as an {@link Object} because its type differs between the
     * supported Minecraft versions; the implementations cast it back.
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    void meteor$onCreateRenderPass(Object backend);
}
