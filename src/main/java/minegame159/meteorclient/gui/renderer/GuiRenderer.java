/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.renderer;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.renderer.operations.TextOperation;
import minegame159.meteorclient.gui.renderer.packer.GuiTexture;
import minegame159.meteorclient.gui.renderer.packer.TexturePacker;
import minegame159.meteorclient.gui.renderer.packer.TextureRegion;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.rendering.DrawMode;
import minegame159.meteorclient.rendering.MeshBuilder;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.Pool;
import minegame159.meteorclient.utils.render.ByteTexture;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static minegame159.meteorclient.utils.Utils.getWindowHeight;
import static minegame159.meteorclient.utils.Utils.getWindowWidth;
import static org.lwjgl.opengl.GL11.*;

public class GuiRenderer {
    private static final Color WHITE = new Color(255, 255, 255);

    private static final TexturePacker TEXTURE_PACKER = new TexturePacker();
    private static ByteTexture TEXTURE;

    public static GuiTexture CIRCLE;
    public static GuiTexture TRIANGLE;
    public static GuiTexture EDIT;
    public static GuiTexture RESET;

    public GuiTheme theme;

    private final MeshBuilder mb = new MeshBuilder();
    private final MeshBuilder mbTex = new MeshBuilder();

    private final Pool<Scissor> scissorPool = new Pool<>(Scissor::new);
    private final Stack<Scissor> scissorStack = new Stack<>();

    private final Pool<TextOperation> textPool = new Pool<>(TextOperation::new);
    private final List<TextOperation> texts = new ArrayList<>();

    private final List<Runnable> postTasks = new ArrayList<>();

    public String tooltip, lastTooltip;
    public WWidget tooltipWidget;
    private double tooltipAnimProgress;

    public static GuiTexture addTexture(Identifier id) {
        return TEXTURE_PACKER.add(id);
    }

    public static void init() {
        CIRCLE = addTexture(new Identifier("meteor-client", "textures/icons/gui/circle.png"));
        TRIANGLE = addTexture(new Identifier("meteor-client", "textures/icons/gui/triangle.png"));
        EDIT = addTexture(new Identifier("meteor-client", "textures/icons/gui/edit.png"));
        RESET = addTexture(new Identifier("meteor-client", "textures/icons/gui/reset.png"));

        TEXTURE = TEXTURE_PACKER.pack();
    }

    public void begin() {
        glEnable(GL_SCISSOR_TEST);
        scissorStart(0, 0, getWindowWidth(), getWindowHeight());
    }

    public void end() {
        scissorEnd();

        for (Runnable task : postTasks) task.run();
        postTasks.clear();

        glDisable(GL_SCISSOR_TEST);
    }

    private void beginRender() {
        mb.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR);
        mbTex.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR_TEXTURE);
    }

    private void endRender() {
        mb.end();
        TEXTURE.bindTexture();
        mbTex.texture = true;
        mbTex.end();

        // Normal text
        theme.textRenderer().begin(theme.scale(1));
        for (TextOperation text : texts) {
            if (!text.title) text.run(textPool);
        }
        theme.textRenderer().end();

        // Title text
        theme.textRenderer().begin(theme.scale(1.25));
        for (TextOperation text : texts) {
            if (text.title) text.run(textPool);
        }
        theme.textRenderer().end();

        texts.clear();
    }

    public void scissorStart(double x, double y, double width, double height) {
        if (!scissorStack.isEmpty()) {
            Scissor parent = scissorStack.peek();

            if (x < parent.x) x = parent.x;
            else if (x + width > parent.x + parent.width) width -= (x + width) - (parent.x + parent.width);

            if (y < parent.y) y = parent.y;
            else if (y + height > parent.y + parent.height) height -= (y + height) - (parent.y + parent.height);

            parent.apply();
            endRender();
        }

        scissorStack.push(scissorPool.get().set(x, y, width, height));
        beginRender();
    }

    public void scissorEnd() {
        Scissor scissor = scissorStack.pop();

        scissor.apply();
        endRender();
        for (Runnable task : scissor.postTasks) task.run();
        if (!scissorStack.isEmpty()) beginRender();

        scissorPool.free(scissor);
    }

    public boolean renderTooltip(double mouseX, double mouseY, double delta) {
        tooltipAnimProgress += (tooltip != null ? 1 : -1) * delta * 14;
        tooltipAnimProgress = Utils.clamp(tooltipAnimProgress, 0, 1);

        boolean toReturn = false;

        if (tooltipAnimProgress > 0) {
            if (tooltip != null && !tooltip.equals(lastTooltip)) {
                tooltipWidget = theme.tooltip(tooltip);
                tooltipWidget.init();
            }

            tooltipWidget.move(-tooltipWidget.x + mouseX + 12, -tooltipWidget.y + mouseY + 12);

            setAlpha(tooltipAnimProgress);

            begin();
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
        mb.alpha = a;
        mbTex.alpha = a;

        theme.textRenderer().setAlpha(a);
    }

    public void tooltip(String text) {
        tooltip = text;
    }

    public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
        mb.quad(x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
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
        mbTex.texQuad(x, y, width, height, texture.get(width, height), color);
    }

    public void rotatedQuad(double x, double y, double width, double height, double rotation, GuiTexture texture, Color color) {
        TextureRegion region = texture.get(width, height);

        double rad = Math.toRadians(rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double oX = x + width / 2;
        double oY = y + height / 2;

        double _x1 = ((x - oX) * cos) - ((y - oY) * sin) + oX;
        double _y1 = ((y - oY) * cos) + ((x - oX) * sin) + oY;
        mbTex.pos(_x1, _y1, 0).color(color).texture(region.x1, region.y1).endVertex();

        double _x = ((x + width - oX) * cos) - ((y - oY) * sin) + oX;
        double _y = ((y - oY) * cos) + ((x + width - oX) * sin) + oY;
        mbTex.pos(_x, _y, 0).color(color).texture(region.x2, region.y1).endVertex();

        double _x2 = ((x + width - oX) * cos) - ((y + height - oY) * sin) + oX;
        double _y2 = ((y + height - oY) * cos) + ((x + width - oX) * sin) + oY;
        mbTex.pos(_x2, _y2, 0).color(color).texture(region.x2, region.y2).endVertex();

        mbTex.pos(_x1, _y1, 0).color(color).texture(region.x1, region.y1).endVertex();

        mbTex.pos(_x2, _y2, 0).color(color).texture(region.x2, region.y2).endVertex();

        _x = ((x - oX) * cos) - ((y + height - oY) * sin) + oX;
        _y = ((y + height - oY) * cos) + ((x - oX) * sin) + oY;
        mbTex.pos(_x, _y, 0).color(color).texture(region.x1, region.y2).endVertex();
    }

    public void text(String text, double x, double y, Color color, boolean title) {
        texts.add(getOp(textPool, x, y, color).set(text, theme.textRenderer(), title));
    }

    public void texture(double x, double y, double width, double height, double rotation, AbstractTexture texture) {
        post(() -> {
            mbTex.begin(null, DrawMode.Triangles, VertexFormats.POSITION_COLOR_TEXTURE);

            mbTex.pos(x, y, 0).color(WHITE).texture(0, 0).endVertex();
            mbTex.pos(x + width, y, 0).color(WHITE).texture(1, 0).endVertex();
            mbTex.pos(x + width, y + height, 0).color(WHITE).texture(1, 1).endVertex();
            mbTex.pos(x, y, 0).color(WHITE).texture(0, 0).endVertex();
            mbTex.pos(x + width, y + height, 0).color(WHITE).texture(1, 1).endVertex();
            mbTex.pos(x, y + height, 0).color(WHITE).texture(0, 1).endVertex();

            texture.bindTexture();
            GL11.glPushMatrix();
            GL11.glTranslated(x + width / 2, y + height / 2, 0);
            GL11.glRotated(rotation, 0, 0, 1);
            GL11.glTranslated(-x - width / 2, -y - height / 2, 0);
            mbTex.end();
            GL11.glPopMatrix();
        });
    }

    public void post(Runnable task) {
        scissorStack.peek().postTasks.add(task);
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
