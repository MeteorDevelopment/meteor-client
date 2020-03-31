package minegame159.meteorclient.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.opengl.GL11;

public class RenderUtils {
     private static Tessellator lineTesselator = new Tessellator(1000);
     private static BufferBuilder lineBuf = lineTesselator.getBuffer();

     private static Tessellator quadTesselator = new Tessellator(1000);
     private static BufferBuilder quadBuf = quadTesselator.getBuffer();

     public static void beginLines(double oX, double oY, double oZ) {
         lineBuf.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
         lineBuf.setOffset(oX, oY, oZ);
     }
     public static void beginLines() {
         beginLines(0, 0, 0);
     }
     public static void endLines() {
         lineTesselator.draw();
     }

     public static void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
         lineBuf.vertex(x1, y1, z1).color(color.r, color.g, color.b, color.a).next();
         lineBuf.vertex(x2, y2, z2).color(color.r, color.g, color.b, color.a).next();
     }
     public static void line(double x1, double y1, double x2, double y2, Color color) {
         line(x1, y1, 0, x2, y2, 0, color);
     }

     public static void boxEdges(double x1, double y1, double z1, double x2, double y2, double z2, Color color, Direction excludeDir) {
         if (excludeDir != Direction.WEST && excludeDir != Direction.NORTH) line(x1, y1, z1, x1, y2, z1, color);
         if (excludeDir != Direction.WEST && excludeDir != Direction.SOUTH) line(x1, y1, z2, x1, y2, z2, color);
         if (excludeDir != Direction.EAST && excludeDir != Direction.NORTH) line(x2, y1, z1, x2, y2, z1, color);
         if (excludeDir != Direction.EAST && excludeDir != Direction.SOUTH) line(x2, y1, z2, x2, y2, z2, color);

         if (excludeDir != Direction.NORTH) line(x1, y1, z1, x2, y1, z1, color);
         if (excludeDir != Direction.NORTH) line(x1, y2, z1, x2, y2, z1, color);
         if (excludeDir != Direction.SOUTH) line(x1, y1, z2, x2, y1, z2, color);
         if (excludeDir != Direction.SOUTH) line(x1, y2, z2, x2, y2, z2, color);

         if (excludeDir != Direction.WEST) line(x1, y1, z1, x1, y1, z2, color);
         if (excludeDir != Direction.WEST) line(x1, y2, z1, x1, y2, z2, color);
         if (excludeDir != Direction.EAST) line(x2, y1, z1, x2, y1, z2, color);
         if (excludeDir != Direction.EAST) line(x2, y2, z1, x2, y2, z2, color);
     }

    public static void boxEdges(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
         boxEdges(x1, y1, z1, x2, y2, z2, color, null);
    }

     public static void blockEdges(int x, int y, int z, Color color, Direction excludeDir) {
        boxEdges(x, y, z, x + 1, y + 1, z + 1, color, excludeDir);
     }
     public static void blockEdges(BlockPos blockPos, Color color) {
         blockEdges(blockPos.getX(), blockPos.getY(), blockPos.getZ(), color, null);
     }

     public static void beginQuads(double oX, double oY, double oZ) {
         quadBuf.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
         quadBuf.setOffset(oX, oY, oZ);
     }
     public static void beginQuads() {
         beginQuads(0, 0, 0);
     }
     public static void endQuads() {
         GlStateManager.disableCull();
         quadTesselator.draw();
     }

     public static void quad(double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, Color color) {
         quadBuf.vertex(x1, y1, z1).color(color.r, color.g, color.b, color.a).next();
         quadBuf.vertex(x2, y2, z2).color(color.r, color.g, color.b, color.a).next();
         quadBuf.vertex(x3, y3, z3).color(color.r, color.g, color.b, color.a).next();
         quadBuf.vertex(x4, y4, z4).color(color.r, color.g, color.b, color.a).next();
     }

     public static void quad(double x, double y, double width, double height, Color color) {
         quad(x, y, 0, x + width, y, 0, x + width, y + height, 0, x, y + height, 0, color);
     }

     public static void boxSides(double x1, double y1, double z1, double x2, double y2, double z2, Color color, Direction excludeDir) {
         if (excludeDir != Direction.DOWN) quad(x1, y1, z1, x1, y1, z2, x2, y1, z2, x2, y1, z1, color); // Bottom
         if (excludeDir != Direction.UP) quad(x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color); // Top

         if (excludeDir != Direction.NORTH) quad(x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color); // Front
         if (excludeDir != Direction.SOUTH) quad(x1, y1, z2, x1, y2, z2, x2, y2, z2, x2, y1, z2, color); // Back

         if (excludeDir != Direction.WEST) quad(x1, y1, z1, x1, y2, z1, x1, y2, z2, x1, y1, z2, color); // Left
         if (excludeDir != Direction.EAST) quad(x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color); // Right
     }

    public static void boxSides(double x1, double y1, double z1, double x2, double y2, double z2, Color color) {
        boxSides(x1, y1, z1, x2, y2, z2, color, null);
    }

     public static void blockSides(int x, int y, int z, Color color, Direction excludeDir) {
        boxSides(x, y, z, x + 1, y + 1, z + 1, color, excludeDir);
     }
     public static void blockSides(BlockPos blockPos, Color color) {
         blockSides(blockPos.getX(), blockPos.getY(), blockPos.getZ(), color, null);
     }

     public static void boxWithLines(double x, double y, double z, double width, double height, Color sideColor, Color lineColor) {
         RenderUtils.quad(x, y, z, x, y, z + height, x + width, y, z + height, x + width, y, z, sideColor);
         RenderUtils.line(x, y, z, x, y, z + height, lineColor);
         RenderUtils.line(x, y, z + height, x + width, y, z + height, lineColor);
         RenderUtils.line(x + width, y, z + height, x + width, y, z, lineColor);
         RenderUtils.line(x, y, z, x + width, y, z, lineColor);
     }
    public static void boxWithLines(double x, double y, double z, Color sideColor, Color lineColor) {
        boxWithLines(x, y, z, 1, 1, sideColor, lineColor);
    }
}
