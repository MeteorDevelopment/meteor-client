package minegame159.meteorclient.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.mixin.BufferRendererAccessor;
import minegame159.meteorclient.utils.Utils;
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
    private final int primitiveVerticesSize;

    private final int vao, vbo, ibo;

    private FloatBuffer vertices;
    private IntBuffer indices;
    private int vertexI, indicesCount;

    private boolean building;

    public Mesh(DrawMode drawMode, Attrib... attributes) {
        int stride = 0;
        for (Attrib attribute : attributes) stride += attribute.size * 4;

        this.drawMode = drawMode;
        this.primitiveVerticesSize = stride / 4 * drawMode.indicesCount;

        vertices = BufferUtils.createFloatBuffer(primitiveVerticesSize * 256);
        indices = BufferUtils.createIntBuffer(drawMode.indicesCount * 512);

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        ibo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);

        int offset = 0;
        for (int i = 0; i < attributes.length; i++) {
            int attribute = attributes[i].size;

            glEnableVertexAttribArray(i);
            glVertexAttribPointer(i, attribute, GL_FLOAT, false, stride, offset);

            offset += attribute * 4;
        }

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        BufferRendererAccessor.setCurrentVertexArray(0);
        BufferRendererAccessor.setCurrentVertexBuffer(0);
        BufferRendererAccessor.setCurrentElementBuffer(0);
    }

    public void begin() {
        if (building) throw new IllegalStateException("Mesh.end() called while already building.");

        vertices.clear();
        indices.clear();

        vertexI = 0;
        indicesCount = 0;

        building = true;
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
        growIfNeeded();
    }

    public void quad(int i1, int i2, int i3, int i4) {
        indices.put(i1);
        indices.put(i2);
        indices.put(i3);

        indices.put(i3);
        indices.put(i4);
        indices.put(i1);

        indicesCount += 6;
        growIfNeeded();
    }

    private void growIfNeeded() {
        // Vertices
        if ((vertexI + 1) * primitiveVerticesSize >= vertices.capacity()) {
            int newSize = vertices.capacity() * 2;
            if (newSize % primitiveVerticesSize != 0) newSize += newSize % primitiveVerticesSize;

            FloatBuffer newVertices = BufferUtils.createFloatBuffer(newSize);

            vertices.flip();
            newVertices.put(vertices);

            vertices = newVertices;
        }

        // Indices
        if (indicesCount >= indices.capacity()) {
            int newSize = indices.capacity() * 2;
            if (newSize % drawMode.indicesCount != 0) newSize += newSize % drawMode.indicesCount;

            IntBuffer newIndices = BufferUtils.createIntBuffer(newSize);

            indices.flip();
            newIndices.put(indices);

            indices = newIndices;
        }
    }

    public void end() {
        if (!building) throw new IllegalStateException("Mesh.end() called while not building.");

        vertices.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        indices.flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        BufferRendererAccessor.setCurrentVertexBuffer(0);
        BufferRendererAccessor.setCurrentElementBuffer(0);

        building = false;
    }

    public void render(MatrixStack matrices, boolean rendering3D) {
        if (building) end();

        if (indicesCount > 0) {
            // Setup opengl state and matrix stack
            if (!depthTest) glDisable(GL_DEPTH_TEST);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glDisable(GL_CULL_FACE);
            glEnable(GL_LINE_SMOOTH);
            glLineWidth(1);

            MatrixStack matrixStack = RenderSystem.getModelViewStack();

            if (rendering3D) {
                matrixStack.push();

                Vec3d cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
                if (matrices != null) matrixStack.method_34425(matrices.peek().getModel());
                matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            }

            // Render
            beforeRender();
            Shader.BOUND.setDefaults();

            glBindVertexArray(vao);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
            glDrawElements(drawMode.getGL(), indicesCount, GL_UNSIGNED_INT, 0);

            // Cleanup opengl state and matrix stack
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            glBindVertexArray(0);

            BufferRendererAccessor.setCurrentElementBuffer(0);
            BufferRendererAccessor.setCurrentVertexArray(0);

            if (rendering3D) matrixStack.pop();

            glDisable(GL_LINE_SMOOTH);
            glEnable(GL_CULL_FACE);
            glDisable(GL_BLEND);
            if (!depthTest) glEnable(GL_DEPTH_TEST);
        }
    }

    public void render(MatrixStack matrices) {
        render(matrices, Utils.rendering3D);
    }

    protected void beforeRender() {}
}
