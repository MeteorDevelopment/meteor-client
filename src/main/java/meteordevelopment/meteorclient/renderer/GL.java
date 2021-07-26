/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.GlStateManager;
import meteordevelopment.meteorclient.mixin.BufferRendererAccessor;
import meteordevelopment.meteorclient.mixininterface.ICapabilityTracker;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import org.lwjgl.BufferUtils;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static meteordevelopment.meteorclient.utils.Utils.mc;
import static org.lwjgl.opengl.GL32C.*;

public class GL {
    private static final FloatBuffer MAT = BufferUtils.createFloatBuffer(4 * 4);

    private static final ICapabilityTracker DEPTH = getTracker("DEPTH");
    private static final ICapabilityTracker BLEND = getTracker("BLEND");
    private static final ICapabilityTracker CULL = getTracker("CULL");

    private static boolean depthSaved, blendSaved, cullSaved;

    private static boolean changeBufferRenderer = true;

    public static void init() {
        if (FabricLoader.getInstance().isModLoaded("canvas")) changeBufferRenderer = false;
    }

    // Generation

    public static int genVertexArray() {
        return GlStateManager._glGenVertexArrays();
    }

    public static int genBuffer() {
        return GlStateManager._glGenBuffers();
    }

    // Binding

    public static void bindVertexArray(int vao) {
        GlStateManager._glBindVertexArray(vao);
        if (changeBufferRenderer) BufferRendererAccessor.setCurrentVertexArray(vao);
    }

    public static void bindVertexBuffer(int vbo) {
        GlStateManager._glBindBuffer(GL_ARRAY_BUFFER, vbo);
        if (changeBufferRenderer) BufferRendererAccessor.setCurrentVertexBuffer(vbo);
    }

    public static void bindIndexBuffer(int ibo) {
        GlStateManager._glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        if (changeBufferRenderer) BufferRendererAccessor.setCurrentElementBuffer(ibo);
    }

    // Buffers

    public static void bufferData(int target, ByteBuffer data, int usage) {
        GlStateManager._glBufferData(target, data, usage);
    }

    public static void drawElements(int mode, int first, int type) {
        GlStateManager._drawElements(mode, first, type, 0);
    }

    // Vertex attributes

    public static void enableVertexAttribute(int i) {
        GlStateManager._enableVertexAttribArray(i);
    }

    public static void vertexAttribute(int index, int size, int type, boolean normalized, int stride, long pointer) {
        GlStateManager._vertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    // Shaders

    public static int createShader(int type) {
        return GlStateManager.glCreateShader(type);
    }

    public static void shaderSource(int shader, String source) {
        GlStateManager.glShaderSource(shader, ImmutableList.of(source));
    }

    public static String compileShader(int shader) {
        GlStateManager.glCompileShader(shader);

        if (GlStateManager.glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            return GlStateManager.glGetShaderInfoLog(shader, 512);
        }

        return null;
    }

    public static int createProgram() {
        return GlStateManager.glCreateProgram();
    }

    public static String linkProgram(int program, int vertShader, int fragShader) {
        GlStateManager.glAttachShader(program, vertShader);
        GlStateManager.glAttachShader(program, fragShader);
        GlStateManager.glLinkProgram(program);

        if (GlStateManager.glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            return GlStateManager.glGetProgramInfoLog(program, 512);
        }

        return null;
    }

    public static void deleteShader(int shader) {
        GlStateManager.glDeleteShader(shader);
    }

    public static void useProgram(int program) {
        GlStateManager._glUseProgram(program);
    }

    // Uniforms

    public static int getUniformLocation(int program, String name) {
        return GlStateManager._glGetUniformLocation(program, name);
    }

    public static void uniformInt(int location, int v) {
        GlStateManager._glUniform1i(location, v);
    }

    public static void uniformFloat(int location, float v) {
        glUniform1f(location, v);
    }

    public static void uniformFloat2(int location, float v1, float v2) {
        glUniform2f(location, v1, v2);
    }

    public static void uniformMatrix(int location, Matrix4f v) {
        v.writeColumnMajor(MAT);
        GlStateManager._glUniformMatrix4(location, false, MAT);
    }

    // Textures

    public static void pixelStore(int name, int param) {
        GlStateManager._pixelStore(name, param);
    }

    public static void textureParam(int target, int name, int param) {
        GlStateManager._texParameter(target, name, param);
    }

    public static void textureImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer pixels) {
        glTexImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
    }

    // State

    public static void saveState() {
        depthSaved = DEPTH.get();
        blendSaved = BLEND.get();
        cullSaved = CULL.get();
    }

    public static void restoreState() {
        DEPTH.set(depthSaved);
        BLEND.set(blendSaved);
        CULL.set(cullSaved);

        disableLineSmooth();
    }

    public static void enableDepth() {
        GlStateManager._enableDepthTest();
    }
    public static void disableDepth() {
        GlStateManager._disableDepthTest();
    }

    public static void enableBlend() {
        GlStateManager._enableBlend();
        GlStateManager._blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }
    public static void disableBlend() {
        GlStateManager._disableBlend();
    }

    public static void enableCull() {
        GlStateManager._enableCull();
    }
    public static void disableCull() {
        GlStateManager._disableCull();
    }

    public static void enableLineSmooth() {
        glEnable(GL_LINE_SMOOTH);
        glLineWidth(1);
    }
    public static void disableLineSmooth() {
        glDisable(GL_LINE_SMOOTH);
    }

    public static void bindTexture(Identifier id) {
        GlStateManager._activeTexture(GL_TEXTURE0);
        mc.getTextureManager().bindTexture(id);
    }

    public static void bindTexture(int i) {
        GlStateManager._activeTexture(GL_TEXTURE0);
        GlStateManager._bindTexture(i);
    }

    private static ICapabilityTracker getTracker(String fieldName) {
        try {
            Class<?> glStateManager = GlStateManager.class;

            Field field = glStateManager.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object state = field.get(null);

            String trackerName = FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", "com.mojang.blaze3d.platform.GlStateManager$class_1018");

            Field capStateField = null;
            for (Field f : state.getClass().getDeclaredFields()) {
                if (f.getType().getName().equals(trackerName)) {
                    capStateField = f;
                    break;
                }
            }

            capStateField.setAccessible(true);
            return (ICapabilityTracker) capStateField.get(state);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
