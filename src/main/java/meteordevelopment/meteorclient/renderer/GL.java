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
import org.lwjgl.opengl.GL33C;

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
    private static final ICapabilityTracker SCISSOR = getTracker("SCISSOR");

    private static boolean depthSaved, blendSaved, cullSaved, scissorSaved;

    private static boolean changeBufferRenderer = true;

    public static void init() {
        if (FabricLoader.getInstance().isModLoaded("canvas")) changeBufferRenderer = false;
    }

    public static void clearErrors() {
        while (GL33C.glGetError() != GL33C.GL_NO_ERROR) {}
    }

    public static void checkError(String name) {
        if (GL33C.glGetError() != GL33C.GL_NO_ERROR) System.out.println("GL ERROR AT: " + name);
    }

    // Generation

    public static int genVertexArray() {
        return GlStateManager._glGenVertexArrays();
    }

    public static int genBuffer() {
        clearErrors();
        int a = GlStateManager._glGenBuffers();
        checkError("genBuffer");
        return a;
    }

    public static int genTexture() {
        clearErrors();
        int a = GlStateManager._genTexture();
        checkError("genTexture");
        return a;
    }

    public static int genFramebuffer() {
        clearErrors();
        int a = GlStateManager.glGenFramebuffers();
        checkError("genFramebuffer");
        return a;
    }

    // Deletion

    public static void deleteShader(int shader) {
        clearErrors();
        GlStateManager.glDeleteShader(shader);
        checkError("deleteShader");
    }

    public static void deleteTexture(int id) {
        clearErrors();
        GlStateManager._deleteTexture(id);
        checkError("deleteTexture");
    }

    public static void deleteFramebuffer(int fbo) {
        clearErrors();
        GlStateManager._glDeleteFramebuffers(fbo);
        checkError("deleteFramebuffer");
    }

    // Binding

    public static void bindVertexArray(int vao) {
        clearErrors();
        GlStateManager._glBindVertexArray(vao);
        if (changeBufferRenderer) BufferRendererAccessor.setCurrentVertexArray(vao);
        checkError("bindVertexArray");
    }

    public static void bindVertexBuffer(int vbo) {
        clearErrors();
        GlStateManager._glBindBuffer(GL_ARRAY_BUFFER, vbo);
        if (changeBufferRenderer) BufferRendererAccessor.setCurrentVertexBuffer(vbo);
        checkError("bindVertexBuffer");
    }

    public static void bindIndexBuffer(int ibo) {
        clearErrors();
        GlStateManager._glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
        if (changeBufferRenderer) BufferRendererAccessor.setCurrentElementBuffer(ibo);
        checkError("bindIndexBuffer");
    }

    public static void bindFramebuffer(int fbo) {
        clearErrors();
        GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        checkError("bindIndexBuffer");
    }

    // Buffers

    public static void bufferData(int target, ByteBuffer data, int usage) {
        clearErrors();
        GlStateManager._glBufferData(target, data, usage);
        checkError("bufferData");
    }

    public static void drawElements(int mode, int first, int type) {
        clearErrors();
        GlStateManager._drawElements(mode, first, type, 0);
        checkError("drawElements");
    }

    // Vertex attributes

    public static void enableVertexAttribute(int i) {
        clearErrors();
        GlStateManager._enableVertexAttribArray(i);
        checkError("enableVertexAttribute");
    }

    public static void vertexAttribute(int index, int size, int type, boolean normalized, int stride, long pointer) {
        clearErrors();
        GlStateManager._vertexAttribPointer(index, size, type, normalized, stride, pointer);
        checkError("vertexAttribute");
    }

    // Shaders

    public static int createShader(int type) {
        clearErrors();
        int a = GlStateManager.glCreateShader(type);
        checkError("createShader");
        return a;
    }

    public static void shaderSource(int shader, String source) {
        clearErrors();
        GlStateManager.glShaderSource(shader, ImmutableList.of(source));
        checkError("shaderSource");
    }

    public static String compileShader(int shader) {
        clearErrors();
        GlStateManager.glCompileShader(shader);

        if (GlStateManager.glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            return GlStateManager.glGetShaderInfoLog(shader, 512);
        }

        checkError("compileShader");
        return null;
    }

    public static int createProgram() {
        clearErrors();
        int a = GlStateManager.glCreateProgram();
        checkError("createProgram");
        return a;
    }

    public static String linkProgram(int program, int vertShader, int fragShader) {
        clearErrors();
        GlStateManager.glAttachShader(program, vertShader);
        GlStateManager.glAttachShader(program, fragShader);
        GlStateManager.glLinkProgram(program);

        if (GlStateManager.glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            return GlStateManager.glGetProgramInfoLog(program, 512);
        }

        checkError("linkProgram");
        return null;
    }

    public static void useProgram(int program) {
        clearErrors();
        GlStateManager._glUseProgram(program);
        checkError("useProgram");
    }

    // Uniforms

    public static int getUniformLocation(int program, String name) {
        clearErrors();
        int a = GlStateManager._glGetUniformLocation(program, name);
        checkError("getUniformLocation");
        return a;
    }

    public static void uniformInt(int location, int v) {
        clearErrors();
        GlStateManager._glUniform1i(location, v);
        checkError("uniformInt");
    }

    public static void uniformFloat(int location, float v) {
        clearErrors();
        glUniform1f(location, v);
        checkError("uniformFloat");
    }

    public static void uniformFloat2(int location, float v1, float v2) {
        clearErrors();
        glUniform2f(location, v1, v2);
        checkError("uniformFloat2");
    }

    public static void uniformMatrix(int location, Matrix4f v) {
        clearErrors();
        v.writeColumnMajor(MAT);
        GlStateManager._glUniformMatrix4(location, false, MAT);
        checkError("uniformMatrix");
    }

    // Textures

    public static void pixelStore(int name, int param) {
        clearErrors();
        GlStateManager._pixelStore(name, param);
        checkError("pixelStore");
    }

    public static void textureParam(int target, int name, int param) {
        clearErrors();
        GlStateManager._texParameter(target, name, param);
        checkError("textureParam");
    }

    public static void textureImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, ByteBuffer pixels) {
        clearErrors();
        glTexImage2D(target, level, internalFormat, width, height, border, format, type, pixels);
        checkError("textureImage2D");
    }

    public static void defaultPixelStore() {
        clearErrors();
        pixelStore(GL_UNPACK_SWAP_BYTES, GL_FALSE);
        pixelStore(GL_UNPACK_LSB_FIRST, GL_FALSE);
        pixelStore(GL_UNPACK_ROW_LENGTH, 0);
        pixelStore(GL_UNPACK_IMAGE_HEIGHT, 0);
        pixelStore(GL_UNPACK_SKIP_ROWS, 0);
        pixelStore(GL_UNPACK_SKIP_PIXELS, 0);
        pixelStore(GL_UNPACK_SKIP_IMAGES, 0);
        pixelStore(GL_UNPACK_ALIGNMENT, 4);
        checkError("defaultPixelStore");
    }

    // Framebuffers

    public static void framebufferTexture2D(int target, int attachment, int textureTarget, int texture, int level) {
        clearErrors();
        GlStateManager._glFramebufferTexture2D(target, attachment, textureTarget, texture, level);
        checkError("framebufferTexture2D");
    }

    // State

    public static void saveState() {
        clearErrors();
        depthSaved = DEPTH.get();
        blendSaved = BLEND.get();
        cullSaved = CULL.get();
        scissorSaved = SCISSOR.get();
        checkError("saveState");
    }

    public static void restoreState() {
        clearErrors();
        DEPTH.set(depthSaved);
        BLEND.set(blendSaved);
        CULL.set(cullSaved);
        SCISSOR.set(scissorSaved);

        disableLineSmooth();
        checkError("restoreState");
    }

    public static void enableDepth() {
        clearErrors();
        GlStateManager._enableDepthTest();
        checkError("enableDepth");
    }
    public static void disableDepth() {
        clearErrors();
        GlStateManager._disableDepthTest();
        checkError("enableDepth");
    }

    public static void enableBlend() {
        clearErrors();
        GlStateManager._enableBlend();
        GlStateManager._blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        checkError("enableBlend");
    }
    public static void disableBlend() {
        clearErrors();
        GlStateManager._disableBlend();
        checkError("disableBlend");
    }

    public static void enableCull() {
        clearErrors();
        GlStateManager._enableCull();
        checkError("enableCull");
    }
    public static void disableCull() {
        clearErrors();
        GlStateManager._disableCull();
        checkError("disableCull");
    }

    public static void enableScissorTest() {
        clearErrors();
        GlStateManager._enableScissorTest();
        checkError("enableScissorTest");
    }
    public static void disableScissorTest() {
        clearErrors();
        GlStateManager._disableScissorTest();
        checkError("disableScissorTest");
    }

    public static void enableLineSmooth() {
        clearErrors();
        glEnable(GL_LINE_SMOOTH);
        glLineWidth(1);
        checkError("enableLineSmooth");
    }
    public static void disableLineSmooth() {
        clearErrors();
        glDisable(GL_LINE_SMOOTH);
        checkError("disableLineSmooth");
    }

    public static void bindTexture(Identifier id) {
        clearErrors();
        GlStateManager._activeTexture(GL_TEXTURE0);
        mc.getTextureManager().bindTexture(id);
        checkError("bindTexture");
    }

    public static void bindTexture(int i) {
        clearErrors();
        GlStateManager._activeTexture(GL_TEXTURE0);
        GlStateManager._bindTexture(i);
        checkError("bindTexture");
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
