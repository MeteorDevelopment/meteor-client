/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.renderer;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.operations.TextOperation;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.renderer.packer.TexturePacker;
import meteordevelopment.meteorclient.gui.renderer.packer.TextureRegion;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class GuiRenderer {
    private static final Color WHITE = new Color(255, 255, 255);

    private static final TexturePacker TEXTURE_PACKER = new TexturePacker();
    private static Texture TEXTURE;

    public static GuiTexture CIRCLE;
    public static GuiTexture TRIANGLE;
    public static GuiTexture EDIT;
    public static GuiTexture RESET;
    public static GuiTexture FAVORITE_NO, FAVORITE_YES;
    public static GuiTexture COPY, PASTE;

    public GuiTheme theme;

    private final Renderer2D r = new Renderer2D(false);
    private final Renderer2D rTex = new Renderer2D(true);

    private final Pool<Scissor> scissorPool = new Pool<>(Scissor::new);
    private final Stack<Scissor> scissorStack = new ObjectArrayList<>();

    private final Pool<TextOperation> textPool = new Pool<>(TextOperation::new);
    private final List<TextOperation> texts = new ObjectArrayList<>();

    private final List<Runnable> postTasks = new ObjectArrayList<>();

    public String tooltip, lastTooltip;
    public WWidget tooltipWidget;
    private double tooltipAnimProgress;

    private GuiGraphicsExtractor graphics;

    public static GuiTexture addTexture(Identifier id) {
        return TEXTURE_PACKER.add(id);
    }

    @PostInit
    public static void init() {
        CIRCLE = addTexture(MeteorClient.identifier("textures/icons/gui/circle.png"));
        TRIANGLE = addTexture(MeteorClient.identifier("textures/icons/gui/triangle.png"));
        EDIT = addTexture(MeteorClient.identifier("textures/icons/gui/edit.png"));
        RESET = addTexture(MeteorClient.identifier("textures/icons/gui/reset.png"));
        FAVORITE_NO = addTexture(MeteorClient.identifier("textures/icons/gui/favorite_no.png"));
        FAVORITE_YES = addTexture(MeteorClient.identifier("textures/icons/gui/favorite_yes.png"));

        COPY = addTexture(MeteorClient.identifier("textures/icons/gui/copy.png"));
        PASTE = addTexture(MeteorClient.identifier("textures/icons/gui/paste.png"));

        TEXTURE = TEXTURE_PACKER.pack();
    }

    public void begin(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
        this.graphics.nextStratum();

        var matrices = graphics.pose();
        matrices.pushMatrix();
        matrices.scale(1.0f / mc.getWindow().getGuiScale());

        scissorStart(0, 0, getWindowWidth(), getWindowHeight());
    }

    public void end() {
        scissorEnd();

        for (Runnable task : postTasks) task.run();
        postTasks.clear();

        graphics.pose().popMatrix();
        graphics.nextStratum();
    }

    // --- Soft glow ---

    private static final int GLOW_FALLOFF = 48;
    private static net.minecraft.client.renderer.texture.DynamicTexture glowTexture;

    // Glow under everything (panel halos) and glow over solid fills (hover highlights).
    private final Renderer2D rGlowUnder = new Renderer2D(true);
    private final Renderer2D rGlowOver = new Renderer2D(true);

    /**
     * Draws a soft glow: a blurred copy of the rect, from a generated falloff texture drawn
     * 9-sliced so the corners stay round at any size.
     *
     * The falloff starts underneath the rect, so the visible halo begins already faded
     * instead of as a bright band hugging the edge (which reads as a border), and the centre
     * is filled so a glow behind a translucent shape shows no inner ring either.
     *
     * @param size how far the glow reaches beyond the rect
     */
    public void glow(double x, double y, double w, double h, double size, Color color, boolean onTop) {
        addGlowQuads(onTop ? rGlowOver : rGlowUnder, x, y, w, h, size, color);
    }

    /** Shared with the HUD renderer so widgets glow exactly like the GUI. */
    public static void addGlowQuads(Renderer2D batch, double x, double y, double w, double h, double size, Color color) {
        if (w <= 0 || h <= 0 || size <= 0 || color.a <= 0) return;
        ensureGlowTexture();

        double hidden = Math.min(size * 0.6, Math.min(w, h) / 2);
        double reach = hidden + size;
        double gx = x + hidden, gy = y + hidden, gw = w - hidden * 2, gh = h - hidden * 2;

        int texels = GLOW_FALLOFF * 2 + 1;
        double edge = (GLOW_FALLOFF + 0.5) / texels;

        double[] xs = {gx - reach, gx, gx + gw, gx + gw + reach};
        double[] ys = {gy - reach, gy, gy + gh, gy + gh + reach};
        double[] uv = {0, edge, edge, 1};

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                double qw = xs[col + 1] - xs[col];
                double qh = ys[row + 1] - ys[row];
                if (qw <= 0 || qh <= 0) continue;

                batch.texQuad(xs[col], ys[row], qw, qh, 0, uv[col], uv[row], uv[col + 1], uv[row + 1], color);
            }
        }
    }

    public static void ensureGlowTexture() {
        if (glowTexture != null) return;

        int texels = GLOW_FALLOFF * 2 + 1;
        com.mojang.blaze3d.platform.NativeImage image = new com.mojang.blaze3d.platform.NativeImage(texels, texels, true);

        for (int py = 0; py < texels; py++) {
            for (int px = 0; px < texels; px++) {
                double dx = px - GLOW_FALLOFF, dy = py - GLOW_FALLOFF;
                double t = Math.min(Math.sqrt(dx * dx + dy * dy) / GLOW_FALLOFF, 1);

                // Zero slope at the outer edge, so the halo fades out with no visible rim.
                double alpha = Math.pow(1 - t, 2.5);
                image.setPixelABGR(px, py, ((int) Math.round(alpha * 255) << 24) | 0xFFFFFF);
            }
        }

        glowTexture = new net.minecraft.client.renderer.texture.DynamicTexture(() -> "ember-glow", image);
        mc.getTextureManager().register(MeteorClient.identifier("ember_glow"), glowTexture);
    }

    public static void renderGlow(Renderer2D batch) {
        if (glowTexture == null) return;
        batch.render(glowTexture.getTextureView(),
            com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().getClampToEdge(com.mojang.blaze3d.textures.FilterMode.LINEAR));
    }

    public void beginRender() {
        rGlowUnder.begin();
        r.begin();
        rGlowOver.begin();
        rTex.begin();
    }

    public void endRender() {
        endRender(null);
    }

    public void endRender(Scissor scissor) {
        if (scissor != null) scissor.push();

        rGlowUnder.end();
        r.end();
        rGlowOver.end();
        rTex.end();

        renderGlow(rGlowUnder);
        r.render();
        renderGlow(rGlowOver);
        rTex.render("u_Texture", TEXTURE.getTextureView(), TEXTURE.getSampler());

        // Normal text
        theme.textRenderer().begin(graphics, theme.scale(1));
        for (TextOperation text : texts) {
            if (!text.title) text.run(textPool);
        }
        theme.textRenderer().end();

        // Title text
        theme.textRenderer().begin(graphics, theme.scale(1.25));
        for (TextOperation text : texts) {
            if (text.title) text.run(textPool);
        }
        theme.textRenderer().end();

        texts.clear();

        if (scissor != null) scissor.pop();
    }

    public void scissorStart(double x, double y, double width, double height) {
        if (!scissorStack.isEmpty()) {
            Scissor parent = scissorStack.top();

            if (x < parent.x) x = parent.x;
            else if (x + width > parent.x + parent.width) width -= (x + width) - (parent.x + parent.width);

            if (y < parent.y) y = parent.y;
            else if (y + height > parent.y + parent.height) height -= (y + height) - (parent.y + parent.height);

            endRender(parent);
        }

        scissorStack.push(scissorPool.get().set(x, y, width, height));
        graphics.enableScissor((int) x, (int) y, (int) (x + width), (int) (y + height));

        beginRender();
    }

    public void scissorEnd() {
        Scissor scissor = scissorStack.pop();

        endRender(scissor);

        scissor.push();
        for (Runnable task : scissor.postTasks) task.run();
        scissor.pop();

        graphics.disableScissor();
        if (!scissorStack.isEmpty()) beginRender();

        scissorPool.free(scissor);
    }

    public boolean renderTooltip(GuiGraphicsExtractor graphics, double mouseX, double mouseY, double delta) {
        tooltipAnimProgress += (tooltip != null ? 1 : -1) * delta * 14;
        tooltipAnimProgress = Mth.clamp(tooltipAnimProgress, 0, 1);

        boolean toReturn = false;

        if (tooltipAnimProgress > 0) {
            if (tooltip != null && !tooltip.equals(lastTooltip)) {
                tooltipWidget = theme.tooltip(tooltip);
                tooltipWidget.init();
            }

            double deltaX = -tooltipWidget.x + mouseX + 12;
            double deltaY = -tooltipWidget.y + mouseY + 12;

            if (mouseX + 12 + tooltipWidget.width > getWindowWidth())
                deltaX = -tooltipWidget.x + getWindowWidth() - tooltipWidget.width;
            if (mouseY + 12 + tooltipWidget.height > getWindowHeight())
                deltaY = -tooltipWidget.y + getWindowHeight() - tooltipWidget.height;

            tooltipWidget.move(deltaX, deltaY);

            setAlpha(tooltipAnimProgress);

            begin(graphics);
            tooltipWidget.render(this, mouseX, mouseY, delta);
            end();

            setAlpha(1);

            lastTooltip = tooltip;
            toReturn = true;
        }

        tooltip = null;
        return toReturn;
    }

    public void setAlpha(double a) {
        r.setAlpha(a);
        rTex.setAlpha(a);

        theme.textRenderer().setAlpha(a);
    }

    public void tooltip(String text) {
        tooltip = text;
    }

    public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
        r.quad(x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void quad(double x, double y, double width, double height, Color colorLeft, Color colorRight) {
        quad(x, y, width, height, colorLeft, colorRight, colorRight, colorLeft);
    }

    public void quad(double x, double y, double width, double height, Color color) {
        quad(x, y, width, height, color, color);
    }

    public void quad(WWidget widget, Color color) {
        quad(widget.x, widget.y, widget.width, widget.height, color);
    }

    public void quad(double x, double y, double width, double height, GuiTexture texture, Color color) {
        rTex.texQuad(x, y, width, height, texture.get(width, height), color);
    }

    public void roundedRect(double x, double y, double w, double h, double rad, Color c) {
        if (rad <= 0) { quad(x, y, w, h, c); return; }
        rad = Math.min(rad, Math.min(w, h) / 2);
        quad(x + rad, y, w - rad * 2, h, c);
        quad(x, y + rad, rad, h - rad * 2, c);
        quad(x + w - rad, y + rad, rad, h - rad * 2, c);
        TextureRegion region = CIRCLE.get(rad * 2, rad * 2);
        double mu = (region.x1 + region.x2) / 2;
        double mv = (region.y1 + region.y2) / 2;
        rTex.texQuad(x, y, rad, rad, 0, region.x1, region.y1, mu, mv, c);
        rTex.texQuad(x + w - rad, y, rad, rad, 0, mu, region.y1, region.x2, mv, c);
        rTex.texQuad(x, y + h - rad, rad, rad, 0, region.x1, mv, mu, region.y2, c);
        rTex.texQuad(x + w - rad, y + h - rad, rad, rad, 0, mu, mv, region.x2, region.y2, c);
    }

    public void rotatedQuad(double x, double y, double width, double height, double rotation, GuiTexture texture, Color color) {
        rTex.texQuad(x, y, width, height, rotation, texture.get(width, height), color);
    }

    public void triangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
        r.triangle(x1, y1, x2, y2, x3, y3, color);
    }

    public void text(String text, double x, double y, Color color, boolean title) {
        texts.add(getOp(textPool, x, y, color).set(text, theme.textRenderer(), title));
    }

    public void texture(double x, double y, double width, double height, double rotation, Texture texture) {
        post(() -> {
            rTex.begin();
            rTex.texQuad(x, y, width, height, rotation, 0, 0, 1, 1, WHITE);
            rTex.end();

            rTex.render(texture.getTextureView(), texture.getSampler());
        });
    }

    public void post(Runnable task) {
        scissorStack.top().postTasks.add(task);
    }

    public void item(ItemStack itemStack, int x, int y, float scale, boolean overlay) {
        RenderUtils.drawItem(graphics, itemStack, x, y, scale, overlay, null, false);
    }

    public void absolutePost(Runnable task) {
        postTasks.add(task);
    }

    private <T extends GuiRenderOperation<T>> T getOp(Pool<T> pool, double x, double y, Color color) {
        T op = pool.get();
        op.set(x, y, color);
        return op;
    }
}
