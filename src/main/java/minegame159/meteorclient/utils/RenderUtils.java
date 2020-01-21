package minegame159.meteorclient.utils;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.lwjgl.opengl.GL11;

public class RenderUtils {
     private static Tessellator lineTesselator = new Tessellator();
     private static BufferBuilder lineBuf = lineTesselator.getBuffer();

     private static Tessellator quadTesselator = new Tessellator();
     private static BufferBuilder quadBuf = quadTesselator.getBuffer();

     public static void beginLines() {
         lineBuf.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
     }
     public static void endLines() {
         lineTesselator.draw();
     }

     public static void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
         lineBuf.vertex(x1, y1, z1).color(color.r, color.g, color.b, color.a).next();
         lineBuf.vertex(x2, y2, z2).color(color.r, color.g, color.b, color.a).next();
     }

     public static void blockEdges(int x, int y, int z, Color color) {
        int x2 = x + 1;
        int y2 = y + 1;
        int z2 = z + 1;

        line(x, y, z, x2, y, z, color);
        line(x, y, z, x, y2, z, color);
        line(x, y, z, x, y, z2, color);

        line(x2, y2, z2, x, y2, z2, color);
        line(x2, y2, z2, x2, y, z2, color);
        line(x2, y2, z2, x2, y2, z, color);

        line(x2, y, z, x2, y2, z, color);
        line(x, y, z2, x, y2, z2, color);

        line(x2, y, z, x2, y, z2, color);
        line(x, y, z2, x2, y, z2, color);

        line(x, y2, z, x2, y2, z, color);
        line(x, y2, z, x, y2, z2, color);
     }

     public static void beginQuads() {
         quadBuf.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
     }
     public static void endQuads() {
         quadTesselator.draw();
     }

     public static void quad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color color) {
         quadBuf.vertex(x1, y1, z1).color(color.r, color.g, color.b, color.a).next();
         quadBuf.vertex(x2, y2, z2).color(color.r, color.g, color.b, color.a).next();
         quadBuf.vertex(x3, y3, z3).color(color.r, color.g, color.b, color.a).next();
         quadBuf.vertex(x4, y4, z4).color(color.r, color.g, color.b, color.a).next();
     }

     public static void blockSides(int x, int y, int z, Color color) {
         quad(x, y, z, x, y, z+1, x+1, y, z+1, x+1, y, z, color); // Bottom
         quad(x, y+1, z, x, y+1, z+1, x+1, y+1, z+1, x+1, y+1, z, color); // Top

         quad(x, y, z, x, y+1, z, x+1, y+1, z, x+1, y, z, color); // Front
         quad(x, y, z+1, x, y+1, z+1, x+1, y+1, z+1, x+1, y, z+1, color); // Back

         quad(x, y, z, x, y+1, z, x, y+1, z+1, x, y, z+1, color); // Left
         quad(x+1, y, z, x+1, y+1, z, x+1, y+1, z+1, x+1, y, z+1, color); // Right
     }
}
