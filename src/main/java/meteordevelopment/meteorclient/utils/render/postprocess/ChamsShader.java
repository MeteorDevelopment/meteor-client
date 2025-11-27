/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.render.postprocess;

import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ResourcePacksReloadedEvent;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Chams;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gl.DynamicUniformStorage;
import net.minecraft.entity.Entity;
import net.minecraft.resource.Resource;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Optional;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ChamsShader extends EntityShader {
    private static final String[] FILE_FORMATS = { "png", "jpg" };

    private static Texture IMAGE_TEX;
    private static Chams chams;

    public ChamsShader() {
        MeteorClient.EVENT_BUS.subscribe(ChamsShader.class);
    }

    @PostInit
    public static void load() {
        try {
            ByteBuffer data = null;
            for (String fileFormat : FILE_FORMATS) {
                Optional<Resource> optional = mc.getResourceManager().getResource(MeteorClient.identifier("textures/chams." + fileFormat));
                if (optional.isEmpty() || optional.get().getInputStream() == null) {
                    continue;
                }

                data = TextureUtil.readResource(optional.get().getInputStream());
                break;
            }
            if (data == null) return;

            data.rewind();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer comp = stack.mallocInt(1);

                STBImage.stbi_set_flip_vertically_on_load(true);
                ByteBuffer image = STBImage.stbi_load_from_memory(data, width, height, comp, 4);

                IMAGE_TEX = new Texture(width.get(0), height.get(0), TextureFormat.RGBA8, FilterMode.NEAREST, FilterMode.NEAREST);
                IMAGE_TEX.upload(image);

                STBImage.stbi_image_free(image);
                STBImage.stbi_set_flip_vertically_on_load(false);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    private static void onResourcePacksReloaded(ResourcePacksReloadedEvent event) {
        load();
    }

    @Override
    protected void setupPass(MeshRenderer renderer) {
        Color color = chams.shaderColor.get();

        renderer.uniform("ImageData", UNIFORM_STORAGE.write(new UniformData(
            color.r / 255f, color.g / 255f, color.b / 255f, color.a / 255f
        )));

        if (chams.isShader() && chams.shader.get() == Chams.Shader.Image && IMAGE_TEX != null) {
            renderer.sampler("u_TextureI", IMAGE_TEX.getGlTextureView());
        }
    }

    @Override
    protected boolean shouldDraw() {
        if (chams == null) chams = Modules.get().get(Chams.class);
        return chams.isShader();
    }

    @Override
    public boolean shouldDraw(Entity entity) {
        if (!shouldDraw()) return false;
        return chams.entities.get().contains(entity.getType()) && (entity != mc.player || !chams.ignoreSelfDepth.get());
    }

    // Uniforms

    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
        .putVec4()
        .get();

    private static final DynamicUniformStorage<UniformData> UNIFORM_STORAGE = new DynamicUniformStorage<>("Meteor - Image UBO", UNIFORM_SIZE, 16);

    public static void flipFrame() {
        UNIFORM_STORAGE.clear();
    }

    private record UniformData(float r, float g, float b, float a) implements DynamicUniformStorage.Uploadable {
        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec4(r, g, b, a);
        }
    }
}
