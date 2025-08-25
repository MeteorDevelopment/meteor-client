/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.config.Config.WindowIcons;
import net.minecraft.SharedConstants;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.Icons;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class IconChanger {
    private IconChanger() {
    }

    public static void setIcon(WindowIcons icon) {
        if (icon == WindowIcons.Default) {
            resetToDefault();
        } else if (icon.icon16 == null || icon.icon32 == null) {
            MeteorClient.LOG.warn("Icon paths cannot be null for custom icons");
        } else {
            setCustomIcon(icon);
        }
    }

    private static void resetToDefault() {
        try {
            mc.getWindow().setIcon(
                mc.getDefaultResourcePack(),
                SharedConstants.getGameVersion().stable() ? Icons.RELEASE : Icons.SNAPSHOT
            );
        } catch (IOException e) {
            MeteorClient.LOG.warn("Failed to reset icon: {}", e.getMessage());
        }
    }

    private static void setCustomIcon(WindowIcons windowIcons) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            GLFWImage.Buffer icons = GLFWImage.malloc(2, stack);

            loadIconSize(windowIcons.icon16, icons, 0, stack);
            loadIconSize(windowIcons.icon32, icons, 1, stack);

            GLFW.glfwSetWindowIcon(mc.getWindow().getHandle(), icons);
        } catch (Exception e) {
            MeteorClient.LOG.warn("Failed to set icon: {}", e.getMessage());
        }
    }

    private static void loadIconSize(Identifier iconPath, GLFWImage.Buffer icons, int index, MemoryStack stack) throws IOException {
        Resource resource = mc.getResourceManager()
            .getResource(iconPath)
            .orElseThrow(() -> new IOException("Icon not found: " + iconPath));

        try (NativeImage image = NativeImage.read(resource.getInputStream())) {
            ByteBuffer pixelBuffer = imageToByteBuffer(image);

            GLFWImage glfwImage = GLFWImage.malloc(stack);
            glfwImage.set(image.getWidth(), image.getHeight(), pixelBuffer);
            icons.put(index, glfwImage);
        }
    }

    private static ByteBuffer imageToByteBuffer(NativeImage image) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * 4);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getColorArgb(x, y);
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                buffer.put((byte) (pixel & 0xFF));         // Blue
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
            }
        }
        buffer.flip();
        return buffer;
    }
}
