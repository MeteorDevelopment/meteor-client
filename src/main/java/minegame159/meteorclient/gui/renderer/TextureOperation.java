package minegame159.meteorclient.gui.renderer;

import minegame159.meteorclient.rendering.MeshBuilder;
import minegame159.meteorclient.utils.Color;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import org.lwjgl.opengl.GL11;

public class TextureOperation extends Operation {
    private static final MeshBuilder TRIANGLES = new MeshBuilder();
    private static final Color WHITE = new Color();

    private double x, y;
    private double width, height;
    private double rotation;
    private AbstractTexture texture;

    public TextureOperation set(double x, double y, double width, double height, double rotation, AbstractTexture texture) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
        this.texture = texture;

        return this;
    }

    @Override
    public void render(GuiRenderer renderer) {
        TRIANGLES.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

        TRIANGLES.pos(x, y, 0).texture(0, 0).color(WHITE).endVertex();
        TRIANGLES.pos(x + width, y, 0).texture(1, 0).color(WHITE).endVertex();
        TRIANGLES.pos(x + width, y + height, 0).texture(1, 1).color(WHITE).endVertex();

        TRIANGLES.pos(x, y, 0).texture(0, 0).color(WHITE).endVertex();
        TRIANGLES.pos(x + width, y + height, 0).texture(1, 1).color(WHITE).endVertex();
        TRIANGLES.pos(x, y + height, 0).texture(0, 1).color(WHITE).endVertex();

        texture.bindTexture();
        GL11.glPushMatrix();
        GL11.glTranslated(x + width / 2, y + height / 2, 0);
        GL11.glRotated(rotation, 0, 0, 1);
        GL11.glTranslated(-x - width / 2, -y - height / 2, 0);
        TRIANGLES.end(true);
        GL11.glPopMatrix();
    }

    @Override
    public void free(GuiRenderer renderer) {
        renderer.textureOperationPool.free(this);
    }
}
