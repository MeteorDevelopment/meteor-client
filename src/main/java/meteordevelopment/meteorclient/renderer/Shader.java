/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static meteordevelopment.meteorclient.utils.Utils.mc;
import static org.lwjgl.opengl.GL32C.*;

public class Shader {
    public static Shader BOUND;

    private static final FloatBuffer MAT = BufferUtils.createFloatBuffer(4 * 4);

    private final int id;
    private final Object2IntMap<String> uniformLocations = new Object2IntOpenHashMap<>();

    public Shader(String vertPath, String fragPath) {
        int vert = glCreateShader(GL_VERTEX_SHADER);
        shaderSource(vert, read(vertPath));
        glCompileShader(vert);

        int[] a = new int[1];
        glGetShaderiv(vert, GL_COMPILE_STATUS, a);
        if (a[0] == GL_FALSE) {
            MeteorClient.LOG.error("Failed to compile vertex shader: " + glGetShaderInfoLog(vert));
            throw new RuntimeException("Failed to compile vertex shader: " + glGetShaderInfoLog(vert));
        }

        int frag = glCreateShader(GL_FRAGMENT_SHADER);
        shaderSource(frag, read(fragPath));
        glCompileShader(frag);

        glGetShaderiv(frag, GL_COMPILE_STATUS, a);
        if (a[0] == GL_FALSE) {
            MeteorClient.LOG.error("Failed to compile fragment shader: " + glGetShaderInfoLog(frag));
            throw new RuntimeException("Failed to compile fragment shader: " + glGetShaderInfoLog(frag));
        }

        id = glCreateProgram();
        glAttachShader(id, vert);
        glAttachShader(id, frag);
        glLinkProgram(id);

        glGetProgramiv(id, GL_LINK_STATUS, a);
        if (a[0] == GL_FALSE) {
            MeteorClient.LOG.error("Failed to link program: " + glGetProgramInfoLog(frag));
            throw new RuntimeException("Failed to link program: " + glGetProgramInfoLog(frag));
        }

        glDeleteShader(vert);
        glDeleteShader(frag);
    }

    // Apparently there is an AMD bug and this supposedly fixes it
    private void shaderSource(int shader, String source) {
        MemoryStack stack = MemoryStack.stackGet();
        int stackPointer = stack.getPointer();

        try {
            ByteBuffer sourceBuffer = MemoryUtil.memUTF8(source, true);

            PointerBuffer pointers = stack.mallocPointer(1);
            pointers.put(sourceBuffer);

            nglShaderSource(shader, 1, pointers.address0(), 0);
            APIUtil.apiArrayFree(pointers.address0(), 1);
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    private String read(String path) {
        try {
            return IOUtils.toString(mc.getResourceManager().getResource(new Identifier("meteor-client", "shaders/" + path)).getInputStream(), StandardCharsets.UTF_8);
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

    public void set(String name, int v) {
        glUniform1i(getLocation(name), v);
    }

    public void set(String name, double v) {
        glUniform1f(getLocation(name), (float) v);
    }

    public void set(String name, double v1, double v2) {
        glUniform2f(getLocation(name), (float) v1, (float) v2);
    }

    public void set(String name, Matrix4f mat) {
        mat.writeColumnMajor(MAT);
        glUniformMatrix4fv(getLocation(name), false, MAT);
    }

    public void setDefaults() {
        set("u_Proj", RenderSystem.getProjectionMatrix());
        set("u_ModelView", RenderSystem.getModelViewStack().peek().getModel());
    }
}
