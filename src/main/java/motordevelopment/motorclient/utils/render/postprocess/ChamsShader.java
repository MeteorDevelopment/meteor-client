/*
 * This file is part of the motor Client distribution (https://github.com/motorDevelopment/motor-client).
 * Copyright (c) motor Development.
 */

package motordevelopment.motorclient.utils.render.postprocess;

import com.mojang.blaze3d.platform.TextureUtil;
import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.events.game.ResourcePacksReloadedEvent;
import motordevelopment.motorclient.renderer.Texture;
import motordevelopment.motorclient.systems.modules.Modules;
import motordevelopment.motorclient.systems.modules.render.Chams;
import motordevelopment.motorclient.utils.PostInit;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.resource.Resource;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Optional;

import static motordevelopment.motorclient.MotorClient.mc;

public class ChamsShader extends EntityShader {
    private static final String[] FILE_FORMATS = { "png", "jpg" };

    private static Texture IMAGE_TEX;
    private static Chams chams;

    public ChamsShader() {
        MotorClient.EVENT_BUS.subscribe(ChamsShader.class);
    }

    @PostInit
    public static void load() {
        try {
            ByteBuffer data = null;
            for (String fileFormat : FILE_FORMATS) {
                Optional<Resource> optional = mc.getResourceManager().getResource(MotorClient.identifier("textures/chams." + fileFormat));
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
                ByteBuffer image = STBImage.stbi_load_from_memory(data, width, height, comp, 3);

                IMAGE_TEX = new Texture();
                IMAGE_TEX.upload(width.get(0), height.get(0), image, Texture.Format.RGB, Texture.Filter.Nearest, Texture.Filter.Nearest, false);

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
    protected void setUniforms() {
        shader.set("u_Color", chams.shaderColor.get());

        if (chams.isShader() && chams.shader.get() == Chams.Shader.Image && IMAGE_TEX != null && IMAGE_TEX.isValid()) {
            IMAGE_TEX.bind(1);
            shader.set("u_TextureI", 1);
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
}
