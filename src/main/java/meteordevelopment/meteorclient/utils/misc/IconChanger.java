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

    public static void setIcon(Identifier iconPath) {
        if (iconPath == null) { // If the default Minecraft icon should be used
            try {
                //Default Minecraft method for setting the windows' icon
                MeteorClient.mc.getWindow().setIcon(
                    MeteorClient.mc.getDefaultResourcePack(),
                    SharedConstants.getGameVersion().isStable() ? Icons.RELEASE : Icons.SNAPSHOT
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        // Retrieve the native window handle of the Minecraft game window
        long windowHandle = MinecraftClient.getInstance().getWindow().getHandle();
        setWindowIcon(windowHandle, iconPath);
    }

    private static void setWindowIcon(long windowHandle, Identifier iconPath) {
        try (MemoryStack stack = MemoryStack.stackPush()) { // Memory stack for temporary native allocations
            // Create buffers to store width, height, and number of channels of the loaded image
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Load the icon image into memory
            ByteBuffer icon = loadIcon(iconPath, w, h, channels);
            if (icon != null) {
                // Create GLFWImage objects to hold the icon's data
                GLFWImage glfwImage1 = GLFWImage.malloc();
                glfwImage1.set(w.get(0), h.get(0), icon); // Set the image dimensions and data
                GLFWImage glfwImage2 = GLFWImage.malloc();
                // Repeat for the second icon (We are doing this twice, because we need to set the icon for the taskbar and window bar)
                glfwImage2.set(w.get(0), h.get(0), icon);

                // Create a buffer to hold multiple icons (for high DPI support)
                GLFWImage.Buffer icons = GLFWImage.malloc(2);
                icons.put(0, glfwImage1); // Add the first icon to the buffer
                icons.put(1, glfwImage2); // Add the second icon to the buffer

                // Set the window icon using GLFW
                org.lwjgl.glfw.GLFW.glfwSetWindowIcon(windowHandle, icons);

                // Free the allocated GLFWImage and buffer memory
                icons.free();
                glfwImage1.free();
                glfwImage2.free();
            } else {
                info("Failed to load icon: " + iconPath);
            }
        }
    }

    private static ByteBuffer loadIcon(Identifier path, IntBuffer w, IntBuffer h, IntBuffer channels) {
        try {
            // Retrieve the resource from the game's resource manager using the provided path
            Resource resource = MeteorClient.mc.getResourceManager()
                .getResource(path)
                .orElseThrow(() -> new IOException("Icon not found: " + path));

            // Open an input stream to read the icon file's raw data
            InputStream inputStream = resource.getInputStream();

            // Read all bytes from the input stream into a byte array
            byte[] iconBytes = inputStream.readAllBytes();

            // Create a direct ByteBuffer to store the raw icon data (necessary for native code)
            ByteBuffer buffer = ByteBuffer.allocateDirect(iconBytes.length).put(iconBytes).flip();

            // Load the image from the byte buffer using STBImage
            ByteBuffer icon = STBImage.stbi_load_from_memory(buffer, w, h, channels, 4); // 4 = RGBA channels
            if (icon == null) {
                // Log an error if the image could not be loaded
                info("Failed to load image from memory for: " + path + " - " + STBImage.stbi_failure_reason());
            }
            return icon; // Return the loaded image
        } catch (IOException e) {
            // Handle IO exceptions during icon loading
            e.printStackTrace();
            return null;
        }
    }

    private static void info(String message) {
        ChatUtils.forceNextPrefixClass(IconChanger.class);
        ChatUtils.info(message);
    }
}
