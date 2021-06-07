/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.gui.renderer;

import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.renderer.operations.TextOperation;
import minegame159.meteorclient.gui.renderer.packer.GuiTexture;
import minegame159.meteorclient.gui.renderer.packer.TexturePacker;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.renderer.Renderer2D;
import minegame159.meteorclient.renderer.Texture;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.misc.Pool;
import minegame159.meteorclient.utils.render.ByteTexture;
import minegame159.meteorclient.utils.render.color.Color;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

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

    private final Renderer2D r = new Renderer2D(false);
    private final Renderer2D rTex = new Renderer2D(true);

    private final Pool<Scissor> scissorPool = new Pool<>(Scissor::new);
    private final Stack<Scissor> scissorStack = new Stack<>();

    private final Pool<TextOperation> textPool = new Pool<>(TextOperation::new);
    private final List<TextOperation> texts = new ArrayList<>();

    private final List<Runnable> postTasks = new ArrayList<>();

    public String tooltip, lastTooltip;
    public WWidget tooltipWidget;
    private double tooltipAnimProgress;

    private MatrixStack matrices;

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

    public void begin(MatrixStack matrices) {
        this.matrices = matrices;

        glEnable(GL_SCISSOR_TEST);
        scissorStart(0, 0, getWindowWidth(), getWindowHeight());
    }

    public void end(MatrixStack matrices) {
        this.matrices = matrices;

        scissorEnd();

        for (Runnable task : postTasks) task.run();
        postTasks.clear();

        glDisable(GL_SCISSOR_TEST);
    }

    private void beginRender() {
        r.begin();
        rTex.begin();
    }

    private void endRender() {
        r.end();
        rTex.end();

        r.render(matrices);

        TEXTURE.bindTexture();
        rTex.render(matrices);

        // Normal text
        theme.textRenderer().begin(theme.scale(1));
        for (TextOperation text : texts) {
            if (!text.title) text.run(textPool);
        }
        theme.textRenderer().end(matrices);

        // Title text
        theme.textRenderer().begin(theme.scale(1.25));
        for (TextOperation text : texts) {
            if (text.title) text.run(textPool);
        }
        theme.textRenderer().end(matrices);

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

    public boolean renderTooltip(double mouseX, double mouseY, double delta, MatrixStack matrices) {
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

            begin(matrices);
            tooltipWidget.render(this, mouseX, mouseY, delta);
            end(matrices);

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

    public void rotatedQuad(double x, double y, double width, double height, double rotation, GuiTexture texture, Color color) {
        rTex.texQuad(x, y, width, height, rotation, texture.get(width, height), color);
    }

    public void text(String text, double x, double y, Color color, boolean title) {
        texts.add(getOp(textPool, x, y, color).set(text, theme.textRenderer(), title));
    }

    public void texture(double x, double y, double width, double height, double rotation, Texture texture) {
        post(() -> {
            rTex.begin();
            rTex.texQuad(x, y, width, height, rotation, 0, 0, 1, 1, WHITE);
            rTex.end();

            texture.bind();
            rTex.render(matrices);
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
