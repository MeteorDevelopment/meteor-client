/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import meteordevelopment.meteorclient.renderer.texture.*;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static meteordevelopment.meteorclient.MeteorClient.*;

public class ImageHud extends HudElement {
    public static final HudElementInfo<ImageHud> INFO = new HudElementInfo<>(Hud.GROUP, "image", "Displays an image.", ImageHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("Scale");

    // General

    private final Setting<String> path = sgGeneral.add(new StringSetting.Builder()
        .name("Path")
        .description("The path/url of the image.")
        .defaultValue("")
        .onChanged(path -> markDirty())
        .build()
    );

    // Scale

    public final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
        .name("custom-scale")
        .description("Applies a custom scale to this hud element.")
        .defaultValue(false)
        .onChanged(aBoolean -> calculateSize())
        .build()
    );

    public final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .visible(customScale::get)
        .defaultValue(1)
        .onChanged(aDouble -> calculateSize())
        .min(0.2)
        .sliderRange(0.2, 3)
        .build()
    );

    private static final int DEBOUNCE_TIME = 40; // In ticks
    public static final Color TRANSPARENT = new Color(255, 255, 255, 255);
    public static final IImageData DEFAULT_IMAGE = new StaticImageData(Identifier.of(MOD_ID,"icon.png"), 64, 64);
    private int debounceCounter = 0;
    private boolean isDirty = false;
    GpuBuffer.MappedView mappedView;
    private IImageData image = DEFAULT_IMAGE;


    public ImageHud() {
        super(INFO);
        calculateSize();
    }

    private float getScale() {
        return customScale.get() ? scale.get().floatValue() : scale.getDefaultValue().floatValue();
    }

    private void calculateSize() {
        setSize(image.width() * getScale(), image.height() * getScale());
    }

    @Override
    public void tick(HudRenderer renderer) {
        if (debounceCounter++ > DEBOUNCE_TIME && isDirty) {
            destroyTexture();
            generateTexture();
            clean();
        }
    }

    @Override
    public void render(HudRenderer renderer) {
        if (image.imageId() == null) return;
        if (image.isAnimated()) {
            mappedView.data().putFloat(0, ((AnimatedImageData) image).getCurrentFrame());
            renderer.animatedTexture(image.imageId(), getX(), getY(),
                image.width() * getScale(), image.height() * getScale(), TRANSPARENT,
                ((AnimatedImageData) image).animBuffer());
        } else {
            renderer.texture(image.imageId(), getX(), getY(),
                image.width() * getScale(), image.height() * getScale(), TRANSPARENT);
            //renderer.texture(imageId,getX(),getY(),width * getScale(),height * getScale(),TRANSPARENT);
        }

    }

    @Override
    public void remove() {
        super.remove();
        destroyTexture();
    }

    private static String parsePath(String path) {
        return path.replace("\"", "").replace("\\", "/");
    }

    private static String parseName(String path) {
        return path.substring(path.lastIndexOf("\\") + 1).replaceAll("[^a-z0-9/._-]","_").toLowerCase();
    }

    private static boolean isGif(ByteBuffer bytes) {
        return bytes.get(0) == 'G' && bytes.get(1) == 'I' && bytes.get(2) == 'F' && bytes.get(3) == '8' &&
            bytes.get(4) == '7' || bytes.get(4) == '9' && bytes.get(5) == 'a';
    }

    private void generateTexture() {
        String parsed = parsePath(path.get());
        String name = parseName(parsed);
        try (InputStream imageFile = parsed.toLowerCase().startsWith("http") ? new URL(parsed).openStream() : new FileInputStream(parsed)) {
            if (imageFile == null) return;
            ByteBuffer imageData = TextureUtil.readResource(imageFile).rewind();
            Identifier imageId = Identifier.of(MOD_ID, "texture_" + name);
            ITextureGetter tex;
            if (isGif(imageData)) {
                tex = AnimatedTexture.readBuffer(imageData, false, FilterMode.LINEAR);
                int[] delays = ((AnimatedTexture) tex).getDelays();
                int duration = Arrays.stream(delays).sum();
                GpuBuffer animBuffer = RenderSystem.getDevice().createBuffer(() -> "anim_buffer_" + name,
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_DST,
                    BufferUtils.createByteBuffer(4));

                image = new AnimatedImageData(
                    imageId,
                    tex.getWidth(),
                    tex.getHeight(),
                    delays,
                    duration,
                    animBuffer
                );
                mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(animBuffer,false,true);
            } else {
                tex = Texture.readBuffer(imageData, false, FilterMode.LINEAR);
                image = new StaticImageData(
                    imageId,
                    tex.getWidth(),
                    tex.getHeight()
                );
            }

            mc.getTextureManager().registerTexture(imageId, (AbstractTexture) tex);
            calculateSize();
        } catch (IOException e) {
            LOG.error("Failed to load texture", e);
        }
        /*
        AnimatedTexture gifTex = AnimatedTexture.readResource(parsed, false, FilterMode.LINEAR);
        if (gifTex == null) return;
        width = gifTex.getWidth();
        height = gifTex.getHeight();
        delays = gifTex.getDelays();
        totalDurationMs = calculateTotalDurationMs(delays);
        imageId = Identifier.of(MOD_ID, "animated_texture_" + name);
        mc.getTextureManager().registerTexture(imageId, gifTex);
        isAnimated = true;
        animBuffer = RenderSystem.getDevice().createBuffer(() -> "anim_buffer_" + name,
            GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE | GpuBuffer.USAGE_COPY_DST,
            BufferUtils.createByteBuffer(4));
        mappedView = RenderSystem.getDevice().createCommandEncoder().mapBuffer(animBuffer,false,true);
        calculateSize();
         */
    }

    private void markDirty() {
        isDirty = true;
        debounceCounter = 0;
    }

    private void clean() {
        isDirty = false;
        debounceCounter = 0;
    }

    private void destroyTexture() {
        //if (image != DEFAULT_IMAGE) mc.getTextureManager().destroyTexture(image.imageId());
        image = DEFAULT_IMAGE;
        calculateSize();
    }


}
    // https://static.wixstatic.com/media/e6f56d_a2b47380e8504300bfb2844e4a8a5159~mv2.gif
    // https://i.redd.it/c2312ilfgmcg1.gif
    // https://i.redd.it/ykq6fxoefwcg1.gif
    // https://meteorclient.com/icon.png
    // https://i.redd.it/2oi7cqxfgmcg1.gif
    // https://i.redd.it/c2312ilfgmcg1.gif
    // https://i.redd.it/g48boe7ggmcg1.gif


