/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Icons;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class IconChanger {

    public static void setIcon(final Identifier iconPath) {
        if (iconPath == null){
            try {
                MeteorClient.mc.getWindow().setIcon(MeteorClient.mc.getDefaultResourcePack(), SharedConstants.getGameVersion().isStable() ? Icons.RELEASE : Icons.SNAPSHOT);
            } catch (final IOException e) {
                e.printStackTrace();
            }
            return;
        }
        final long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
        setWindowIcon(windowHandle, iconPath);
    }

    private static void setWindowIcon(final long windowHandle, final Identifier iconPath) {
        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final IntBuffer w = stack.mallocInt(1);
            final IntBuffer h = stack.mallocInt(1);
            final IntBuffer channels = stack.mallocInt(1);

            final ByteBuffer icon = loadIcon(iconPath, w, h, channels);
            if (icon != null) {
                final GLFWImage glfwImage1 = GLFWImage.malloc();
                glfwImage1.set(w.get(0), h.get(0), icon);
                final GLFWImage glfwImage2 = GLFWImage.malloc();
                glfwImage2.set(w.get(0), h.get(0), icon);

                final GLFWImage.Buffer icons = GLFWImage.malloc(2);
                icons.put(0, glfwImage1);
                icons.put(1, glfwImage2);

                org.lwjgl.glfw.GLFW.glfwSetWindowIcon(windowHandle, icons);

                icons.free();
                glfwImage1.free();
                glfwImage2.free();
            } else {
                info("Failed to load icon: " + iconPath);
            }
        }
    }

    private static ByteBuffer loadIcon(final Identifier path, final IntBuffer w, final IntBuffer h, final IntBuffer channels) {
        try {
            final Resource resource = MeteorClient.mc.getResourceManager().getResource(path).orElseThrow(() -> new IOException("Icon not found: " + path));
            final InputStream inputStream = resource.getInputStream();
            final byte[] iconBytes = inputStream.readAllBytes();
            final ByteBuffer buffer = ByteBuffer.allocateDirect(iconBytes.length).put(iconBytes).flip();
            final ByteBuffer icon = STBImage.stbi_load_from_memory(buffer, w, h, channels, 4);
            if (icon == null) {
                info("Failed to load image from memory for: " + path + " - " + STBImage.stbi_failure_reason());
            }
            return icon;
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private static void info(final String message){
        ChatUtils.forceNextPrefixClass(IconChanger.class);
        ChatUtils.info(message);
    }
}
