/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud.elements;

import meteordevelopment.meteorclient.gui.renderer.packer.TextureRegion;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.misc.texture.ImageData;
import meteordevelopment.meteorclient.utils.misc.texture.ImageDataFactory;
import meteordevelopment.meteorclient.utils.misc.texture.TextureUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.thread.NameableExecutor;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static meteordevelopment.meteorclient.MeteorClient.*;
import static meteordevelopment.meteorclient.utils.misc.texture.TextureUtils.getCurrentAnimationFrame;
import static meteordevelopment.meteorclient.utils.misc.texture.TextureUtils.registerTexture;
import static org.lwjgl.opengl.GL11C.GL_MAX_TEXTURE_SIZE;
import static org.lwjgl.opengl.GL11C.glGetInteger;

public class ImageHud extends HudElement {
    public static final HudElementInfo<ImageHud> INFO = new HudElementInfo<>(Hud.GROUP, "image", "Cures your ADHD.", ImageHud::new);
    public static final int MAX_TEX_SIZE = glGetInteger(GL_MAX_TEXTURE_SIZE);
    private static final Identifier DEFAULT_TEXTURE = Identifier.of(MOD_ID,"textures/icons/gui/default_image.png");
    private static final Identifier LOADING_TEXTURE = Identifier.of(MOD_ID,"textures/icons/gui/loading_image.png");
    private static final Color TRANSPARENT = new Color(255, 255, 255, 255);
    private static final int DEBOUNCE_TIME = 2; // 2 Seconds before rerunning.
    private Identifier texture;
    private final NameableExecutor worker = Util.getIoWorkerExecutor();
    private ImageData cachedImageData;
    private CompletableFuture<Void> currentImageDataFuture;
    private CompletableFuture<Void> debounceTask;
    private long lastModified = System.currentTimeMillis();
    private String lastPath = "";

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public ImageHud() {
        super(INFO);
        setSize(128,128);
    }

    private final Setting<String> path = sgGeneral.add(new StringSetting.Builder()
        .name("Path")
        .description("The full path / link of the image")
        .wide()
        .onChanged(path -> {
           lastModified = System.currentTimeMillis();
           if (debounceTask != null && !debounceTask.isDone()) {
               debounceTask.cancel(false);
           }
           debounceTask = CompletableFuture.runAsync(() -> {
               if (System.currentTimeMillis() - lastModified > DEBOUNCE_TIME && !lastPath.equals(path)) {
                   lastPath = path;
                   composeImage(path);
               }
           }, CompletableFuture.delayedExecutor(DEBOUNCE_TIME, TimeUnit.SECONDS, mc));
        })
        .build()
    );

    public final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Custom scale.")
        .defaultValue(1)
        .min(0.1)
        .sliderRange(0.5, 2)
        .max(10)
        .onChanged(sc -> {
            if (cachedImageData != null) {
                setSize(cachedImageData.width * sc, cachedImageData.height * sc);
            }
        })
        .build()
    );

    /**
     * Composes the image asynchronously, since it can be a very slow process for animated images or big ones.
     * @param path the URI in String format.
     */
    private void composeImage(String path) {
        // Parse URI
        String parsed = path.replace("\"", "").replace("\\", "/");
        String name = parsed.substring(parsed.lastIndexOf("/") + 1);
        cachedImageData = null;

        currentImageDataFuture = CompletableFuture.supplyAsync(() -> {
            try {
                InputStream imageFile = path.toLowerCase().startsWith("http") ? new URL(path).openStream() : new FileInputStream(parsed);
                ImageInputStream stream = ImageIO.createImageInputStream(imageFile);
                ImageReader reader = ImageIO.getImageReaders(stream).next();
                reader.setInput(stream);
                if (reader.getFormatName().equals("gif")) {
                    return ImageDataFactory.fromGIF(name, reader);
                } else {
                    return ImageDataFactory.fromStatic(name, reader);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, worker).exceptionallyAsync( e -> {
            LOG.debug("Failed to load image", e);
            texture = null;
            return null;
        }, mc).thenAcceptAsync(data -> {
            if (data != null) {
                if (texture != null) mc.getTextureManager().destroyTexture(texture);
                texture = registerTexture(data);
                setSize(data.width * scale.get(), data.height * scale.get());
                cachedImageData = data;
            }
        },mc);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (currentImageDataFuture != null && !currentImageDataFuture.isDone()) {
            renderer.texture(LOADING_TEXTURE, getX(), getY(), 128, 128, TRANSPARENT, scale.get().floatValue());
        }
        else if (cachedImageData == null || texture == null) {
            renderer.texture(DEFAULT_TEXTURE,getX(),getY(),128,128,TRANSPARENT,scale.get().floatValue());
        }
        else {
            try {
                if (cachedImageData.delays.isEmpty()){
                    renderer.texture(texture, getX(), getY(), cachedImageData.width, cachedImageData.height, TRANSPARENT, scale.get().floatValue());
                } else {
                    renderGif(renderer, cachedImageData, texture, x, y, scale.get().floatValue());
                }
            } catch (Exception e) {
                LOG.debug("Failed to render image", e);
            }
        }
    }

    public static void renderGif(HudRenderer renderer, ImageData imageData, Identifier texture, int x, int y, float scale) {
        int frameIndex = getCurrentAnimationFrame(imageData.delays);
        int row = frameIndex % imageData.framesPerColumn;
        int column = frameIndex / imageData.framesPerColumn;
        TextureRegion textureRegion = new TextureRegion(imageData.width,imageData.height);

        textureRegion.x1 = (float) (column * imageData.width) / imageData.canvasWidth;
        textureRegion.y1 = (float) (row * imageData.height) / imageData.canvasHeight;
        textureRegion.x2 = (float) ((column + 1) * imageData.width) / imageData.canvasWidth;
        textureRegion.y2 = (float) ((row + 1) * imageData.height) / imageData.canvasHeight;

        renderer.texture(texture,x,y,imageData.width,imageData.height,textureRegion,TRANSPARENT,scale);
    }
}
