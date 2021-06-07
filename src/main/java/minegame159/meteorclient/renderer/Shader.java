package minegame159.meteorclient.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.util.math.Matrix4f;
import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL32C.*;

public class Shader {
    public static Shader BOUND;

    private static final FloatBuffer MAT = BufferUtils.createFloatBuffer(4 * 4);

    private final int id;
    private final Object2IntMap<String> uniformLocations = new Object2IntOpenHashMap<>();

    public Shader(String vertPath, String fragPath) {
        int vert = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vert, read(vertPath));
        glCompileShader(vert);

        int[] a = new int[1];
        glGetShaderiv(vert, GL_COMPILE_STATUS, a);
        if (a[0] == GL_FALSE) throw new RuntimeException("Failed to compile vertex shader: " + glGetShaderInfoLog(vert));

        int frag = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(frag, read(fragPath));
        glCompileShader(frag);

        glGetShaderiv(frag, GL_COMPILE_STATUS, a);
        if (a[0] == GL_FALSE) throw new RuntimeException("Failed to compile fragment shader: " + glGetShaderInfoLog(frag));

        id = glCreateProgram();
        glAttachShader(id, vert);
        glAttachShader(id, frag);
        glLinkProgram(id);

        glDeleteShader(vert);
        glDeleteShader(frag);
    }

    private String read(String path) {
        try {
            return IOUtils.toString(Shader.class.getResourceAsStream("/assets/meteor-client/shaders/" + path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public void bind() {
        glUseProgram(id);
        BOUND = this;
    }

    private int getLocation(String name) {
        if (uniformLocations.containsKey(name)) return uniformLocations.getInt(name);

        int location = glGetUniformLocation(id, name);
        uniformLocations.put(name, location);
        return location;
    }

    public void set(String name, Matrix4f mat) {
        mat.writeColumnMajor(MAT);
        glUniformMatrix4fv(getLocation(name), false, MAT);
    }

    public void set(String name, double v) {
        glUniform1f(getLocation(name), (float) v);
    }

    public void setDefaults() {
        set("u_Proj", RenderSystem.getProjectionMatrix());
        set("u_ModelView", RenderSystem.getModelViewStack().peek().getModel());
    }
}
