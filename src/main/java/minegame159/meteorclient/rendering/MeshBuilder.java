package minegame159.meteorclient.rendering;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.mixininterface.IBufferBuilder;
import minegame159.meteorclient.utils.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import org.lwjgl.opengl.GL11;

public class MeshBuilder {
    private final BufferBuilder buffer;
    private double offsetX, offsetY, offsetZ;

    public MeshBuilder(int initialCapacity) {
        buffer = new BufferBuilder(initialCapacity);
    }

    public MeshBuilder() {
        this(2097152);
    }

    public void begin(RenderEvent event, int drawMode, VertexFormat vertexFormat) {
        if (event != null) {
            offsetX = -event.offsetX;
            offsetY = -event.offsetY;
            offsetZ = -event.offsetZ;
        } else {
            offsetX = 0;
            offsetY = 0;
            offsetZ = 0;
        }

        buffer.begin(drawMode, vertexFormat);
    }
    public void begin(int drawMode, VertexFormat vertexFormat) {
        begin(null, drawMode, vertexFormat);
    }

    public MeshBuilder pos(double x, double y, double z) {
        buffer.vertex(x + offsetX, y + offsetY, z + offsetZ);
        return this;
    }

    public MeshBuilder texture(double x, double y) {
        buffer.texture((float) (x + offsetX), (float) (y + offsetY));
        return this;
    }

    public MeshBuilder color(Color color) {
        buffer.color(color.r, color.g, color.b, color.a);
        return this;
    }

    public void endVertex() {
        buffer.next();
    }

    public void end(boolean texture) {
        GL11.glPushMatrix();
        RenderSystem.multMatrix(Matrices.getTop());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.disableAlphaTest();
        if (texture) RenderSystem.enableTexture();
        else RenderSystem.disableTexture();
        RenderSystem.disableLighting();
        RenderSystem.disableCull();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        RenderSystem.lineWidth(1);
        RenderSystem.color4f(1, 1, 1, 1);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        buffer.end();
        BufferRenderer.draw(buffer);

        RenderSystem.enableAlphaTest();
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        GL11.glPopMatrix();
    }

    public boolean isBuilding() {
        return ((IBufferBuilder) buffer).isBuilding();
    }
}
