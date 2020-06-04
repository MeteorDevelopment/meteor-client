package minegame159.meteorclient.rendering;

import minegame159.meteorclient.events.RenderEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import org.lwjgl.opengl.GL11;

public class Matrices {
    public static void begin() {
        // Here for newer versions that doesn't use opengl's matrix stack
    }

    public static void push() {
        GL11.glPushMatrix();
    }

    public static void translate(double x, double y, double z) {
        GL11.glTranslated(x, y, z);
    }

    public static void rotate(double angle, double x, double y, double z) {
        GL11.glRotated(angle, x, y, z);
    }

    public static void scale(double x, double y, double z) {
        GL11.glScaled(x, y, z);
    }

    public static void lookAtCamera(RenderEvent event, double x, double y, double z, double width, double height) {
        Matrices.translate((-event.offsetX) + (x + width / 2), (-event.offsetY) + (y + height / 2), (-event.offsetZ) + (z));

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Matrices.rotate(-camera.getYaw(), 0, 1, 0);
        Matrices.rotate(camera.getPitch(), 1, 0, 0);

        Matrices.translate((-x - width / 2) + (event.offsetX) + (x), (-y - height / 2) + (event.offsetY) + (y), (-z) + (event.offsetZ) + (z));
    }

    public static void pop() {
        GL11.glPopMatrix();
    }
}
