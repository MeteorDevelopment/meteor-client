/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.renderer;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.renderer.packer.TextureRegion;
import meteordevelopment.meteorclient.gui.renderer.packer.TexturePacker;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.misc.Pool;
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

    private final Pool<Scissor> scissorPool = new Pool<>(Scissor::new);
    private final Stack<Scissor> scissorStack = new ObjectArrayList<>();

    private final List<Runnable> postTasks = new ObjectArrayList<>();

    public String tooltip, lastTooltip;
    public WWidget tooltipWidget;
    private double tooltipAnimProgress;

    private GuiGraphicsExtractor drawContext;
    private double alpha = 1;

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

    public void begin(GuiGraphicsExtractor drawContext) {
        this.drawContext = drawContext;
        this.drawContext.nextStratum();

        var matrices = drawContext.pose();
        matrices.pushMatrix();

        scissorStart(0, 0, getWindowWidth(), getWindowHeight());
    }

    public void end() {
        scissorEnd();

        for (Runnable task : postTasks) task.run();
        postTasks.clear();

        drawContext.pose().popMatrix();
        drawContext.nextStratum();
    }

    public void beginRender() {
        // Native GUI drawing is immediate on 26.1, so there is nothing to batch here.
    }

    public void endRender() {
        endRender(null);
    }

    public void endRender(Scissor scissor) {
        // Native GUI drawing is immediate on 26.1, so there is nothing to flush here.
    }

    public void scissorStart(double x, double y, double width, double height) {
        if (!scissorStack.isEmpty()) {
            Scissor parent = scissorStack.top();

            if (x < parent.x) x = parent.x;
            else if (x + width > parent.x + parent.width) width -= (x + width) - (parent.x + parent.width);

            if (y < parent.y) y = parent.y;
            else if (y + height > parent.y + parent.height) height -= (y + height) - (parent.y + parent.height);
        }

        scissorStack.push(scissorPool.get().set(x, y, width, height));
    }

    public void scissorEnd() {
        Scissor scissor = scissorStack.pop();

        for (Runnable task : scissor.postTasks) task.run();

        scissorPool.free(scissor);
    }

    public boolean renderTooltip(GuiGraphicsExtractor drawContext, double mouseX, double mouseY, double delta) {
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

            if (mouseX + 12 + tooltipWidget.width > getWindowWidth()) deltaX = -tooltipWidget.x + getWindowWidth() - tooltipWidget.width;
            if (mouseY + 12 + tooltipWidget.height > getWindowHeight()) deltaY = -tooltipWidget.y + getWindowHeight() - tooltipWidget.height;

            tooltipWidget.move(deltaX, deltaY);

            setAlpha(tooltipAnimProgress);

            begin(drawContext);
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
        alpha = a;
    }

    public void tooltip(String text) {
        tooltip = text;
    }

    public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
        int x1 = toGuiPos(x);
        int y1 = toGuiPos(y);
        int x2 = toGuiPos(x + width);
        int y2 = toGuiPos(y + height);

        if (x1 >= x2 || y1 >= y2) return;

        if (sameColor(cTopLeft, cTopRight) && sameColor(cTopLeft, cBottomLeft) && sameColor(cTopLeft, cBottomRight)) {
            drawContext.fill(x1, y1, x2, y2, toNativeColor(cTopLeft));
            return;
        }

        // Most Meteor widgets use horizontal or vertical gradients. Handle those natively first.
        if (sameColor(cTopLeft, cTopRight) && sameColor(cBottomLeft, cBottomRight)) {
            drawContext.fillGradient(x1, y1, x2, y2, toNativeColor(cTopLeft), toNativeColor(cBottomLeft));
            return;
        }

        if (sameColor(cTopLeft, cBottomLeft) && sameColor(cTopRight, cBottomRight)) {
            int widthPixels = Math.max(1, x2 - x1);

            for (int i = 0; i < widthPixels; i++) {
                double t = widthPixels == 1 ? 0 : (double) i / (widthPixels - 1);
                Color color = lerp(cTopLeft, cTopRight, t);
                drawContext.fill(x1 + i, y1, x1 + i + 1, y2, toNativeColor(color));
            }

            return;
        }

        int heightPixels = Math.max(1, y2 - y1);

        for (int i = 0; i < heightPixels; i++) {
            double t = heightPixels == 1 ? 0 : (double) i / (heightPixels - 1);
            Color left = lerp(cTopLeft, cBottomLeft, t);
            Color right = lerp(cTopRight, cBottomRight, t);

            if (sameColor(left, right)) drawContext.fill(x1, y1 + i, x2, y1 + i + 1, toNativeColor(left));
            else {
                int widthPixels = Math.max(1, x2 - x1);

                for (int j = 0; j < widthPixels; j++) {
                    double u = widthPixels == 1 ? 0 : (double) j / (widthPixels - 1);
                    drawContext.fill(x1 + j, y1 + i, x1 + j + 1, y1 + i + 1, toNativeColor(lerp(left, right, u)));
                }
            }
        }
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
        if (texture == null) {
            quad(x, y, width, height, color);
            return;
        }

        if (texture == CIRCLE) {
            drawCircle(x, y, width, height, color);
            return;
        }

        if (texture == TRIANGLE) {
            drawTriangleTexture(x, y, width, height, 0, color);
            return;
        }

        if (TEXTURE == null) {
            quad(x, y, width, height, color);
            return;
        }

        TextureRegion region = texture.get(width, height);
        if (region == null) {
            quad(x, y, width, height, color);
            return;
        }

        drawContext.blit(
            TEXTURE.getTextureView(),
            TEXTURE.getSampler(),
            toGuiPos(x),
            toGuiPos(y),
            Math.max(1, toGuiSize(width)),
            Math.max(1, toGuiSize(height)),
            (float) region.x1,
            (float) region.y1,
            (float) (region.x2 - region.x1),
            (float) (region.y2 - region.y1)
        );
    }

    public void rotatedQuad(double x, double y, double width, double height, double rotation, GuiTexture texture, Color color) {
        if (texture == TRIANGLE) {
            drawTriangleTexture(x, y, width, height, rotation, color);
            return;
        }

        quad(x, y, width, height, texture, color);
    }

    public void triangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
        fillTriangle(x1, y1, x2, y2, x3, y3, color);
    }

    public void text(String text, double x, double y, Color color, boolean title) {
        double textScale = theme.scale(title ? 2 * GuiTheme.TITLE_TEXT_SCALE : 2);
        float scale = (float) (textScale / mc.getWindow().getGuiScale());
        double scaledX = x + 0.5 * textScale;
        double scaledY = y + 0.5 * textScale;
        int guiX = toGuiPos(scaledX);
        int guiY = toGuiPos(scaledY);

        var matrices = drawContext.pose();
        matrices.pushMatrix();
        matrices.translate(guiX, guiY);
        matrices.scale(scale);
        drawContext.text(mc.font, text, 0, 0, toNativeColor(color), false);
        matrices.popMatrix();
    }

    public void texture(double x, double y, double width, double height, double rotation, Texture texture) {
        post(() -> {
            drawContext.blit(
                texture.getTextureView(),
                texture.getSampler(),
                toGuiPos(x),
                toGuiPos(y),
                Math.max(1, toGuiSize(width)),
                Math.max(1, toGuiSize(height)),
                0,
                0,
                1,
                1
            );
        });
    }

    public void post(Runnable task) {
        scissorStack.top().postTasks.add(task);
    }

    public void item(ItemStack itemStack, int x, int y, float scale, boolean overlay) {
        int guiX = toGuiPos(x);
        int guiY = toGuiPos(y);
        int size = Math.max(1, toGuiSize(16 * scale));
        drawContext.item(itemStack, guiX, guiY, size);
        if (overlay) drawContext.itemDecorations(mc.font, itemStack, guiX, guiY);
    }

    public void absolutePost(Runnable task) {
        postTasks.add(task);
    }

    private void drawCircle(double x, double y, double width, double height, Color color) {
        int x1 = toGuiPos(x);
        int y1 = toGuiPos(y);
        int x2 = toGuiPos(x + width);
        int y2 = toGuiPos(y + height);
        int w = Math.max(1, x2 - x1);
        int h = Math.max(1, y2 - y1);

        double rx = w / 2.0;
        double ry = h / 2.0;
        double cx = x1 + rx;
        double cy = y1 + ry;
        int nativeColor = toNativeColor(color);

        for (int py = y1; py < y2; py++) {
            double dy = ((py + 0.5) - cy) / ry;
            double span = 1 - dy * dy;
            if (span < 0) continue;

            double dx = Math.sqrt(span) * rx;
            int rowStart = (int) Math.floor(cx - dx);
            int rowEnd = (int) Math.ceil(cx + dx);
            drawContext.fill(rowStart, py, rowEnd, py + 1, nativeColor);
        }
    }

    private void drawTriangleTexture(double x, double y, double width, double height, double rotation, Color color) {
        double[] p1 = rotatePoint(x, y, x + width / 2, y + height / 2, rotation);
        double[] p2 = rotatePoint(x + width, y, x + width / 2, y + height / 2, rotation);
        double[] p3 = rotatePoint(x + width / 2, y + height, x + width / 2, y + height / 2, rotation);
        fillTriangle(p1[0], p1[1], p2[0], p2[1], p3[0], p3[1], color);
    }

    private void fillTriangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
        x1 = toGuiCoord(x1);
        y1 = toGuiCoord(y1);
        x2 = toGuiCoord(x2);
        y2 = toGuiCoord(y2);
        x3 = toGuiCoord(x3);
        y3 = toGuiCoord(y3);

        int minX = (int) Math.floor(Math.min(x1, Math.min(x2, x3)));
        int minY = (int) Math.floor(Math.min(y1, Math.min(y2, y3)));
        int maxX = Math.max((int) Math.ceil(Math.max(x1, Math.max(x2, x3))), minX + 1);
        int maxY = Math.max((int) Math.ceil(Math.max(y1, Math.max(y2, y3))), minY + 1);
        int nativeColor = toNativeColor(color);

        double area = edge(x1, y1, x2, y2, x3, y3);
        if (area == 0) return;

        for (int py = minY; py < maxY; py++) {
            for (int px = minX; px < maxX; px++) {
                double sampleX = px + 0.5;
                double sampleY = py + 0.5;

                double w0 = edge(x2, y2, x3, y3, sampleX, sampleY);
                double w1 = edge(x3, y3, x1, y1, sampleX, sampleY);
                double w2 = edge(x1, y1, x2, y2, sampleX, sampleY);

                boolean inside = area > 0
                    ? w0 >= 0 && w1 >= 0 && w2 >= 0
                    : w0 <= 0 && w1 <= 0 && w2 <= 0;

                if (inside) drawContext.fill(px, py, px + 1, py + 1, nativeColor);
            }
        }
    }

    private double edge(double x1, double y1, double x2, double y2, double px, double py) {
        return (px - x1) * (y2 - y1) - (py - y1) * (x2 - x1);
    }

    private double[] rotatePoint(double x, double y, double cx, double cy, double rotation) {
        double radians = Math.toRadians(rotation);
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double dx = x - cx;
        double dy = y - cy;

        return new double[] {
            cx + dx * cos - dy * sin,
            cy + dx * sin + dy * cos
        };
    }

    private boolean sameColor(Color a, Color b) {
        return a.r == b.r && a.g == b.g && a.b == b.b && a.a == b.a;
    }

    private Color lerp(Color a, Color b, double t) {
        return new Color(
            lerpChannel(a.r, b.r, t),
            lerpChannel(a.g, b.g, t),
            lerpChannel(a.b, b.b, t),
            lerpChannel(a.a, b.a, t)
        );
    }

    private int lerpChannel(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }

    private int toNativeColor(Color color) {
        int alphaChannel = (int) Math.round(color.a * alpha);
        alphaChannel = Math.max(0, Math.min(255, alphaChannel));
        return Color.fromRGBA(color.r, color.g, color.b, alphaChannel);
    }

    private double toGuiCoord(double value) {
        return value / mc.getWindow().getGuiScale();
    }

    private int toGuiPos(double value) {
        return (int) Math.round(toGuiCoord(value));
    }

    private int toGuiSize(double value) {
        return Math.max(1, (int) Math.round(toGuiCoord(value)));
    }

    private int toInt(double value) {
        return (int) Math.round(value);
    }
}
