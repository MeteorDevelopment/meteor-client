package minegame159.meteorclient.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.mixin.BufferRendererAccessor;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL32C.*;

public class Mesh {
    public enum Attrib {
        Float(1),
        Vec2(2),
        Vec3(3),
        Color(4);

        public final int size;

        Attrib(int size) {
            this.size = size;
        }
    }

    public boolean depthTest = false;
    public double alpha = 1;

    private final DrawMode drawMode;

    private final int vao, vbo, ibo;

    private FloatBuffer vertices;
    private IntBuffer indices;
    private int vertexI, indicesCount;

    public Mesh(DrawMode drawMode, Attrib... attributes) {
        this.drawMode = drawMode;

        vertices = BufferUtils.createFloatBuffer(2048 * 32);
        indices = BufferUtils.createIntBuffer(2048 * 32);

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        ibo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);

        int stride = 0;
        for (Attrib attribute : attributes) stride += attribute.size * 4;

        int offset = 0;
        for (int i = 0; i < attributes.length; i++) {
            int attribute = attributes[i].size;

            glVertexAttribPointer(i, attribute, GL_FLOAT, false, stride, offset);
            glEnableVertexAttribArray(i);

            offset += attribute * 4;
        }

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        resetBufferBuilderBoundBuffers();
    }

    public void begin() {
        vertices.clear();
        indices.clear();

        vertexI = 0;
        indicesCount = 0;
    }

    public Mesh vec3(double x, double y, double z) {
        vertices.put((float) x);
        vertices.put((float) y);
        vertices.put((float) z);

        return this;
    }

    public Mesh vec2(double x, double y) {
        vertices.put((float) x);
        vertices.put((float) y);

        return this;
    }

    public Mesh color(Color c) {
        vertices.put(c.r / 255f);
        vertices.put(c.g / 255f);
        vertices.put(c.b / 255f);
        vertices.put(c.a / 255f * (float) alpha);

        return this;
    }

    public int next() {
        return vertexI++;
    }

    public void line(int i1, int i2) {
        indices.put(i1);
        indices.put(i2);

        indicesCount += 2;
    }

    public void quad(int i1, int i2, int i3, int i4) {
        indices.put(i1);
        indices.put(i2);
        indices.put(i3);

        indices.put(i3);
        indices.put(i4);
        indices.put(i1);

        indicesCount += 6;
    }

    public void end() {
        vertices.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        indices.flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        resetBufferBuilderBoundBuffers();
    }

    public void render(MatrixStack matrices) {
        if (indicesCount > 0) {
            // Setup opengl state and matrix stack
            if (!depthTest) glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_LINE_SMOOTH);
            glLineWidth(1);

            MatrixStack matrixStack = RenderSystem.getModelViewStack();
            matrixStack.push();

            Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
            matrixStack.method_34425(matrices.peek().getModel());
            matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

            // Render
            beforeRender();
            Shader.BOUND.setDefaults();

            glBindVertexArray(vao);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
            glDrawElements(drawMode.getGL(), indicesCount, GL_UNSIGNED_INT, 0);

            // Cleanup opengl state and matrix stack
            glBindVertexArray(0);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

            matrixStack.pop();

            glDisable(GL_LINE_SMOOTH);
            glDisable(GL_BLEND);
            if (!depthTest) glEnable(GL_DEPTH_TEST);
        }
    }

    protected void beforeRender() {}

    private static void resetBufferBuilderBoundBuffers() {
        BufferRendererAccessor.setCurrentVertexArray(0);
        BufferRendererAccessor.setCurrentVertexBuffer(0);
        BufferRendererAccessor.setCurrentElementBuffer(0);
    }
}
