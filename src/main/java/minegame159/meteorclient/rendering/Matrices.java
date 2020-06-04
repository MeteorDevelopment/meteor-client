package minegame159.meteorclient.rendering;

import minegame159.meteorclient.events.RenderEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Quaternion;

public class Matrices {
    private static MatrixStack matrixStack;

    public static void begin(MatrixStack matrixStack) {
        Matrices.matrixStack = matrixStack;
    }

    public static void push() {
        matrixStack.push();
    }

    public static void translate(double x, double y, double z) {
        matrixStack.translate(x, y, z);
    }

    public static void rotate(double angle, double x, double y, double z) {
        matrixStack.multiply(new Quaternion((float) (x * angle), (float) (y * angle), (float) (z * angle), true));
    }

    public static void scale(double x, double y, double z) {
        matrixStack.scale((float) x, (float) y, (float) z);
    }

    public static void lookAtCamera(RenderEvent event, double x, double y, double z, double width, double height) {
        Matrices.translate((-event.offsetX) + (x + width / 2), (-event.offsetY) + (y + height / 2), (-event.offsetZ) + (z));

        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
        Matrices.rotate(-camera.getYaw(), 0, 1, 0);
        Matrices.rotate(camera.getPitch(), 1, 0, 0);

        Matrices.translate((-x - width / 2) + (event.offsetX) + (x), (-y - height / 2) + (event.offsetY) + (y), (-z) + (event.offsetZ) + (z));
    }

    public static void pop() {
        matrixStack.pop();
    }
}
