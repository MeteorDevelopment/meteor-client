/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.hud;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.CustomFontChangedEvent;
import meteordevelopment.meteorclient.renderer.*;
import meteordevelopment.meteorclient.renderer.text.CustomTextRenderer;
import meteordevelopment.meteorclient.renderer.text.Font;
import meteordevelopment.meteorclient.renderer.text.VanillaTextRenderer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class HudRenderer {
    public static final HudRenderer INSTANCE = new HudRenderer();

    private static final double SCALE_TO_HEIGHT = 1.0 / 18.0;

    private final Hud hud = Hud.get();
    private final List<Runnable> postTasks = new ArrayList<>();

    private final Int2ObjectMap<FontHolder> fontsInUse = new Int2ObjectOpenHashMap<>();
    private final LoadingCache<Integer, FontHolder> fontCache = CacheBuilder.newBuilder()
        .maximumSize(4)
        .expireAfterAccess(Duration.ofMinutes(10))
        .removalListener(notification -> {
            if (notification.wasEvicted())
                ((FontHolder) notification.getValue()).destroy();
        })
        .build(CacheLoader.from(HudRenderer::loadFont));

    public DrawContext drawContext;
    public double delta;

    private HudRenderer() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public void begin(DrawContext drawContext) {
        Renderer2D.COLOR.begin();

        this.drawContext = drawContext;
        this.delta = Utils.frameTime;

        if (!hud.hasCustomFont()) {
            VanillaTextRenderer.INSTANCE.scaleIndividually = true;
            VanillaTextRenderer.INSTANCE.begin();
        }
    }

    public void end() {
        Renderer2D.COLOR.render();

        if (hud.hasCustomFont()) {
            // Render fonts that were visited this frame and move to cache which weren't visited
            for (Iterator<FontHolder> it = fontsInUse.values().iterator(); it.hasNext(); ) {
                FontHolder fontHolder = it.next();

                if (fontHolder.visited) {
                    MeshRenderer.begin()
                        .attachments(mc.getFramebuffer())
                        .pipeline(MeteorRenderPipelines.UI_TEXT)
                        .mesh(fontHolder.getMesh())
                        .setupCallback(pass -> pass.bindSampler("u_Texture", fontHolder.font.texture.getGlTexture()))
                        .end();
                }
                else {
                    it.remove();
                    fontCache.put(fontHolder.font.getHeight(), fontHolder);
                }

                fontHolder.visited = false;
            }
        }
        else {
            VanillaTextRenderer.INSTANCE.end();
            VanillaTextRenderer.INSTANCE.scaleIndividually = false;
        }

        for (Runnable task : postTasks) task.run();
        postTasks.clear();

        this.drawContext = null;
    }

    public void line(double x1, double y1, double x2, double y2, Color color) {
        Renderer2D.COLOR.line(x1, y1, x2, y2, color);
    }

    public void quad(double x, double y, double width, double height, Color color) {
        Renderer2D.COLOR.quad(x, y, width, height, color);
    }

    public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
        Renderer2D.COLOR.quad(x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void triangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
        Renderer2D.COLOR.triangle(x1, y1, x2, y2, x3, y3, color);
    }

    public void texture(Identifier id, double x, double y, double width, double height, Color color) {
        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(x, y, width, height, color);
        Renderer2D.TEXTURE.render(mc.getTextureManager().getTexture(id).getGlTexture());
    }

    public double text(String text, double x, double y, Color color, boolean shadow, double scale) {
        if (scale == -1) scale = hud.getTextScale();

        if (!hud.hasCustomFont()) {
            VanillaTextRenderer.INSTANCE.scale = scale * 2;
            return VanillaTextRenderer.INSTANCE.render(text, x, y, color, shadow);
        }

        FontHolder fontHolder = getFontHolder(scale, true);

        Font font = fontHolder.font;
        MeshBuilder mesh = fontHolder.getMesh();

        double width;

        if (shadow) {
            int preShadowA = CustomTextRenderer.SHADOW_COLOR.a;
            CustomTextRenderer.SHADOW_COLOR.a = (int) (color.a / 255.0 * preShadowA);

            width = font.render(mesh, text, x + 1, y + 1, CustomTextRenderer.SHADOW_COLOR, scale);
            font.render(mesh, text, x, y, color, scale);

            CustomTextRenderer.SHADOW_COLOR.a = preShadowA;
        }
        else {
            width = font.render(mesh, text, x, y, color, scale);
        }

        return width;
    }
    public double text(String text, double x, double y, Color color, boolean shadow) {
        return text(text, x, y, color, shadow, -1);
    }

    public double textWidth(String text, boolean shadow, double scale) {
        if (text.isEmpty()) return 0;

        if (hud.hasCustomFont()) {
            double width = getFont(scale).getWidth(text, text.length());
            return (width + (shadow ? 1 : 0)) * (scale == -1 ? hud.getTextScale() : scale) + (shadow ? 1 : 0);
        }

        VanillaTextRenderer.INSTANCE.scale = (scale == -1 ? hud.getTextScale() : scale) * 2;
        return VanillaTextRenderer.INSTANCE.getWidth(text, shadow);
    }
    public double textWidth(String text, boolean shadow) {
        return textWidth(text, shadow, -1);
    }
    public double textWidth(String text, double scale) {
        return textWidth(text, false, scale);
    }
    public double textWidth(String text) {
        return textWidth(text, false, -1);
    }

    public double textHeight(boolean shadow, double scale) {
        if (hud.hasCustomFont()) {
            double height = getFont(scale).getHeight() + 1;
            return (height + (shadow ? 1 : 0)) * (scale == -1 ? hud.getTextScale() : scale);
        }

        VanillaTextRenderer.INSTANCE.scale = (scale == -1 ? hud.getTextScale() : scale) * 2;
        return VanillaTextRenderer.INSTANCE.getHeight(shadow);
    }
    public double textHeight(boolean shadow) {
        return textHeight(shadow, -1);
    }
    public double textHeight() {
        return textHeight(false, -1);
    }

    public void post(Runnable task) {
        postTasks.add(task);
    }

    public void item(ItemStack itemStack, int x, int y, float scale, boolean overlay, String countOverlay) {
        RenderUtils.drawItem(drawContext, itemStack, x, y, scale, overlay, countOverlay);
    }

    public void item(ItemStack itemStack, int x, int y, float scale, boolean overlay) {
        RenderUtils.drawItem(drawContext, itemStack, x, y, scale, overlay);
    }

    /**
     * Draws an entity to the screen. The default version provided by InventoryScreen has had its parameters changed
     * such that it's no longer appropriate for this use case. As the new version uses rotation based on the mouse
     * position relative to itself, it causes some odd angle positioning that may also look "stuck" to one corner,
     * and the model's facing may change depending on how we reposition the element.
     * Additionally, it uses OpenGL scissors, which causes the player model to get cut when the Minecraft GUI scale is not 1x.
     * This version should fix these issues.
     */
    public void entity(DrawContext context, float x, float y, int size, float yaw, float pitch, LivingEntity entity) {

        float tanYaw = (float) Math.atan((yaw) / 40.0f);
        float tanPitch = (float) Math.atan((pitch) / 40.0f);

        // By default, the origin of the drawEntity command is the top-center, facing down and straight to the south.
        // This means that the player model is upside-down. We'll apply a rotation of PI radians (180 degrees) to fix this.
        // This does have the downside of setting the origin to the bottom-center corner, though, so we'll have
        // to compensate for this later.
        Quaternionf quaternion = new Quaternionf().rotateZ((float) Math.PI);

        // The drawEntity command draws the entity using some entity parameters, so we'll have to manipulate some of
        // those to draw as we want. But first, we'll save the previous values, so we can restore them later.
        float previousBodyYaw = entity.bodyYaw;
        float previousYaw = entity.getYaw();
        float previousPitch = entity.getPitch();
        float lastLastHeadYaw = entity.lastHeadYaw; // A perplexing name, I know!
        float lastHeadYaw = entity.headYaw;

        // Apply the rotation parameters
        entity.bodyYaw = 180.0f + tanYaw * 20.0f;
        entity.setYaw(180.0f + tanYaw * 40.0f);
        entity.setPitch(-tanPitch * 20.0f);
        entity.headYaw = entity.getYaw();
        entity.lastHeadYaw = entity.getYaw();

        // Recall the player's origin is now the bottom-center corner, so we'll have to offset the draw by half the width
        // to get it to render in the center.
        // As for the y parameter, adding the element's height draws it at the bottom, but in practice we want the player
        // to "float" somewhat, so we'll multiply it by some constant to have it hover. It turns out 0.9 is a good value.
        // The vector3 parameter applies a translation to the player's model. Given that we're simply offsetting
        // the draw in the x and y parameters, we won't really need this, so we'll set it to default.
        // It doesn't seem like quaternionf2 does anything, so we'll leave it null to save some computation.
        InventoryScreen.drawEntity(context, x, y, size, new Vector3f(), quaternion, null, entity);

        // Restore the previous values
        entity.bodyYaw = previousBodyYaw;
        entity.setYaw(previousYaw);
        entity.setPitch(previousPitch);
        entity.lastHeadYaw = lastLastHeadYaw;
        entity.headYaw = lastHeadYaw;
    }

    private FontHolder getFontHolder(double scale, boolean render) {
        // Calculate font height
        if (scale == -1) scale = hud.getTextScale();
        int height = (int) Math.round(scale / SCALE_TO_HEIGHT);

        // Check fonts in use
        FontHolder fontHolder = fontsInUse.get(height);
        if (fontHolder != null) {
            if (render) fontHolder.visited = true;
            return fontHolder;
        }

        // Create font if not in cache otherwise remove from cache and add to fonts in use
        if (render) {
            fontHolder = fontCache.getIfPresent(height);
            if (fontHolder == null) fontHolder = loadFont(height);
            else fontCache.invalidate(height);

            fontsInUse.put(height, fontHolder);
            fontHolder.visited = true;

            return fontHolder;
        }

        // Otherwise get from cache
        return fontCache.getUnchecked(height);
    }

    private Font getFont(double scale) {
        return getFontHolder(scale, false).font;
    }

    @EventHandler
    private void onCustomFontChanged(CustomFontChangedEvent event) {
        // Need to destroy both fonts in use and in cache because they were not evicted from the cache automatically
        for (FontHolder fontHolder : fontsInUse.values()) fontHolder.destroy();
        for (FontHolder fontHolder : fontCache.asMap().values()) fontHolder.destroy();

        // Clear collections
        fontsInUse.clear();
        fontCache.invalidateAll();
    }

    private static FontHolder loadFont(int height) {
        byte[] data = Utils.readBytes(Fonts.RENDERER.fontFace.toStream());
        ByteBuffer buffer = BufferUtils.createByteBuffer(data.length).put(data).flip();

        return new FontHolder(new Font(buffer, height));
    }

    private static class FontHolder {
        public final Font font;
        public boolean visited;

        private MeshBuilder mesh;

        public FontHolder(Font font) {
            this.font = font;
        }

        public MeshBuilder getMesh() {
            if (mesh == null) mesh = new MeshBuilder(MeteorRenderPipelines.UI_TEXT);
            if (!mesh.isBuilding()) mesh.begin();
            return mesh;
        }

        public void destroy() {
            font.texture.close();
        }
    }
}
