package minegame159.meteorclient.rendering;

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

    public static void pop() {
        GL11.glPopMatrix();
    }
}
