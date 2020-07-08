package minegame159.meteorclient.rendering;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.events.RenderEvent;
import minegame159.meteorclient.mixininterface.IBufferBuilder;
import minegame159.meteorclient.utils.Color;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.VertexFormat;
import org.lwjgl.opengl.GL11;

public class MeshBuilder {
    private static final BufferRenderer RENDERER = new BufferRenderer();

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
        buffer.texture(x + offsetX, y + offsetY);
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
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableDepthTest();
        GlStateManager.disableAlphaTest();
        if (texture) GlStateManager.enableTexture();
        else GlStateManager.disableTexture();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GlStateManager.lineWidth(1);
        GlStateManager.color4f(1, 1, 1, 1);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        buffer.end();
        RENDERER.draw(buffer);

        GlStateManager.disableBlend();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableDepthTest();
        GlStateManager.enableTexture();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
    }

    public boolean isBuilding() {
        return ((IBufferBuilder) buffer).isBuilding();
    }
}
