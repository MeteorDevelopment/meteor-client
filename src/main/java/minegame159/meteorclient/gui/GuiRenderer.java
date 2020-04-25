package minegame159.meteorclient.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.gui.widgets.Cell;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Pool;
import minegame159.meteorclient.utils.TextureRegion;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GuiRenderer {
    private static final Color DEBUG_COLOR_WIDGET = new Color(25, 25, 225);
    private static final Color DEBUG_COLOR_CELL = new Color(25, 225, 25);

    private static Identifier TEXTURE = new Identifier("meteor-client", "gui.png");
    private static int TEXTURE_WIDTH = 97;
    private static int TEXTURE_HEIGHT = 32;

    public static TextureRegion TEX_QUAD = new TextureRegion(TEXTURE_WIDTH, TEXTURE_HEIGHT, 0, 0, 1, 1, null, null, null);
    public static TextureRegion TEX_RESET = new TextureRegion(TEXTURE_WIDTH, TEXTURE_HEIGHT, 1, 0, 32, 32, GuiConfig.INSTANCE.reset, GuiConfig.INSTANCE.resetHovered, GuiConfig.INSTANCE.resetPressed);
    public static TextureRegion TEX_SLIDER_HANDLE = new TextureRegion(TEXTURE_WIDTH, TEXTURE_HEIGHT, 33, 0, 32, 32, GuiConfig.INSTANCE.sliderHandle, GuiConfig.INSTANCE.sliderHandleHovered, GuiConfig.INSTANCE.sliderHandlePressed);
    public static TextureRegion TEX_EDIT = new TextureRegion(TEXTURE_WIDTH, TEXTURE_HEIGHT, 65, 0, 32, 32, GuiConfig.INSTANCE.edit, GuiConfig.INSTANCE.editHovered, GuiConfig.INSTANCE.editPressed);

    private static Tessellator lineTesselator = new Tessellator(1000);
    private static BufferBuilder lineBuf = lineTesselator.getBuffer();

    private static Tessellator quadTesselator = new Tessellator(1000);
    private static BufferBuilder quadBuf = quadTesselator.getBuffer();

    private Pool<QuadOperation> quadOperationPool = new Pool<>(QuadOperation::new);
    private Pool<LineOperation> lineOperationPool = new Pool<>(LineOperation::new);
    private Pool<TriangleOperation> triangleOperationPool = new Pool<>(TriangleOperation::new);
    private Pool<ItemOperation> itemOperationPool = new Pool<>(ItemOperation::new);
    private Pool<TextOperation> textOperationPool = new Pool<>(TextOperation::new);
    private Pool<ScissorOperation> scissorOperationPool = new Pool<>(ScissorOperation::new);
    private Pool<TextScissorOperation> textScissorOperationPool = new Pool<>(TextScissorOperation::new);

    private List<Operation> operations = new ArrayList<>();
    private List<Operation> postOperations = new ArrayList<>();
    private List<TextOperation> textOperations = new ArrayList<>();
    private TextOperation tooltip;

    private boolean preScissorTest = false;
    private boolean preTextScissorTest = false;

    private int textOperationI;

    public void begin() {
        // Lines
        lineBuf.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);

        // Quads
        quadBuf.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
    }

    public void renderLine(double x1, double y1, double x2, double y2, Color color) {
        LineOperation operation = lineOperationPool.get();
        operation.x1 = x1;
        operation.y1 = y1;
        operation.x2 = x2;
        operation.y2 = y2;
        operation.color = color;
        operations.add(operation);
    }

    public void renderQuad(double x, double y, double width, double height, TextureRegion tex, Color color1, Color color2, Color color3, Color color4) {
        QuadOperation o = quadOperationPool.get();
        o.x = x;
        o.y = y;
        o.width = width;
        o.height = height;
        o.tex = tex;
        o.color1 = color1;
        o.color2 = color2;
        o.color3 = color3;
        o.color4 = color4;
        operations.add(o);
    }
    public void renderQuad(double x, double y, double width, double height, TextureRegion tex, Color color) {
        renderQuad(x, y, width, height, tex, color, color, color, color);
    }
    public void renderQuad(double x, double y, double width, double height, Color colorLeft, Color colorRight) {
        renderQuad(x, y, width, height, null, colorLeft, colorRight, colorRight, colorLeft);
    }
    public void renderQuad(double x, double y, double width, double height, Color color) {
        renderQuad(x, y, width, height, null, color, color, color, color);
    }

    public void renderTriangle(double x, double y, double size, double angle, Color color) {
        TriangleOperation o = triangleOperationPool.get();
        o.x = x;
        o.y = y;
        o.size = size;
        o.angle = angle;
        o.color = color;
        operations.add(o);
    }

    public void renderItem(double x, double y, ItemStack itemStack) {
        ItemOperation o = itemOperationPool.get();
        o.x = x;
        o.y = y;
        o.itemStack = itemStack;
        postOperations.add(o);
    }

    private void endBuffers() {
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.enableTexture();
        MinecraftClient.getInstance().getTextureManager().bindTexture(TEXTURE);
        quadTesselator.draw();

        GlStateManager.disableTexture();
        lineTesselator.draw();
    }

    public void end() {
        GlStateManager.enableBlend();
        GlStateManager.disableCull();

        GlStateManager.pushMatrix();
        GL11.glLineWidth(1);

        preScissorTest = false;
        preTextScissorTest = false;

        // Render quads and lines
        for (Operation operation : operations) {
            operation.render();
            operation.free();
        }
        operations.clear();
        endBuffers();

        // Render post operations
        for (Operation operation : postOperations) {
            operation.render();
            operation.free();
        }
        postOperations.clear();

        // Render text
        GlStateManager.enableTexture();
        for (textOperationI = 0; textOperationI < textOperations.size(); textOperationI++) {
            TextOperation textOperation = textOperations.get(textOperationI);

            textOperation.render();
            textOperation.free();
        }
        textOperations.clear();

        // Render tooltip
        if (tooltip != null) {
            tooltip.render();
            tooltip.free();
            tooltip = null;
        }

        GlStateManager.popMatrix();

        GlStateManager.enableCull();
        GlStateManager.disableBlend();
    }

    public void renderBackground(WWidget widget, boolean hovered, boolean pressed) {
        Color background = GuiConfig.INSTANCE.background;
        Color outline = GuiConfig.INSTANCE.outline;

        if (pressed) {
            background = GuiConfig.INSTANCE.backgroundPressed;
            outline = GuiConfig.INSTANCE.outlinePressed;
        } else if (hovered) {
            background = GuiConfig.INSTANCE.backgroundHovered;
            outline = GuiConfig.INSTANCE.outlineHovered;
        }

        renderQuad(widget.x, widget.y, widget.width, widget.height, background);
        renderQuad(widget.x, widget.y, widget.width, 1, outline);
        renderQuad(widget.x, widget.y + widget.height - 1, widget.width, 1, outline);
        renderQuad(widget.x, widget.y, 1, widget.height, outline);
        renderQuad(widget.x + widget.width - 1, widget.y, 1, widget.height, outline);
    }

    public void renderText(String text, double x, double y, Color color, boolean shadow) {
        TextOperation o = textOperationPool.get();
        o.text = text;
        o.x = x;
        o.y = y;
        o.color = color;
        o.shadow = shadow;
        textOperations.add(o);
    }

    public void renderTooltip(String text, double x, double y, Color color) {
        TextOperation o = textOperationPool.get();
        o.text = text;
        o.x = x;
        o.y = y;
        o.color = color;
        o.shadow = true;
        tooltip = o;
    }

    public void startTextScissor(WWidget widget, double padTop, double padRight, double padBottom, double padLeft) {
        TextScissorOperation o = textScissorOperationPool.get();
        double scaleFactor = MinecraftClient.getInstance().window.getScaleFactor();
        o.x = (widget.x + padLeft) * scaleFactor;
        o.y = (MinecraftClient.getInstance().window.getScaledHeight() - widget.y - widget.height + padTop) * scaleFactor;
        o.width = (widget.width - padLeft - padRight) * scaleFactor;
        o.height = (widget.height - padTop - padBottom) * scaleFactor;
        o.start = true;
        textOperations.add(o);
    }

    public void endTextScissor() {
        TextScissorOperation operation = textScissorOperationPool.get();
        operation.start = false;
        textOperations.add(operation);
    }

    public void startScissor(WWidget widget, double padTop, double padRight, double padBottom, double padLeft) {
        ScissorOperation o1 = scissorOperationPool.get();
        ScissorOperation o2 = scissorOperationPool.get();

        double scaleFactor = MinecraftClient.getInstance().window.getScaleFactor();
        o1.x = o2.x = (widget.x + padLeft) * scaleFactor;
        o1.y = o2.y = (MinecraftClient.getInstance().window.getScaledHeight() - widget.y - widget.height + padTop) * scaleFactor;
        o1.width = o2.width = (widget.width - padLeft - padRight) * scaleFactor;
        o1.height = o2.height = (widget.height - padTop - padBottom) * scaleFactor;
        o1.start = o2.start = true;

        o1.resetQuadLine = true;
        operations.add(o1);

        o2.resetQuadLine = false;
        postOperations.add(o2);
    }

    public void endScissor() {
        ScissorOperation o1 = scissorOperationPool.get();
        ScissorOperation o2 = scissorOperationPool.get();

        o1.start = o2.start = false;

        o1.resetQuadLine = true;
        operations.add(o1);

        o2.resetQuadLine = false;
        postOperations.add(o2);
    }

    public void renderDebug(WWidget widget) {
        renderLine(widget.x, widget.y, widget.x + widget.width, widget.y, DEBUG_COLOR_WIDGET);
        renderLine(widget.x, widget.y + widget.height, widget.x + widget.width, widget.y + widget.height, DEBUG_COLOR_WIDGET);
        renderLine(widget.x, widget.y, widget.x, widget.y + widget.height, DEBUG_COLOR_WIDGET);
        renderLine(widget.x + widget.width, widget.y, widget.x + widget.width, widget.y + widget.height, DEBUG_COLOR_WIDGET);

        for (Cell<?> cell : widget.getCells()) {
            renderLine(cell.getX(), cell.getY(), cell.getX() + cell.getWidth(), cell.getY(), DEBUG_COLOR_CELL);
            renderLine(cell.getX(), cell.getY() + cell.getHeight(), cell.getX() + cell.getWidth(), cell.getY() + cell.getHeight(), DEBUG_COLOR_CELL);
            renderLine(cell.getX(), cell.getY(), cell.getX(), cell.getY() + cell.getHeight(), DEBUG_COLOR_CELL);
            renderLine(cell.getX() + cell.getWidth(), cell.getY(), cell.getX() + cell.getWidth(), cell.getY() + cell.getHeight(), DEBUG_COLOR_CELL);

            renderDebug(cell.getWidget());
        }
    }

    private static abstract class Operation {
        abstract void render();

        abstract void free();
    }

    private class QuadOperation extends Operation {
        double x, y;
        double width, height;
        TextureRegion tex;
        Color color1, color2, color3, color4;

        @Override
        void render() {
            TextureRegion tex = this.tex;
            if (tex == null) tex = TEX_QUAD;
            
            quadBuf.vertex(x, y, 0).texture(tex.x, tex.y).color(color1.r, color1.g, color1.b, color1.a).next();
            quadBuf.vertex(x + width, y, 0).texture(tex.x + tex.width, tex.y).color(color2.r, color2.g, color2.b, color2.a).next();
            quadBuf.vertex(x + width, y + height, 0).texture(tex.x + tex.width, tex.y + tex.height).color(color3.r, color3.g, color3.b, color3.a).next();
            quadBuf.vertex(x, y + height, 0).texture(tex.x, tex.y + tex.height).color(color4.r, color4.g, color4.b, color4.a).next();
        }

        @Override
        void free() {
            quadOperationPool.free(this);
        }
    }

    private class TriangleOperation extends Operation {
        double x, y;
        double size;
        double angle;
        Color color;

        @Override
        void render() {
            double cos = Math.cos(Math.toRadians(angle));
            double sin = Math.sin(Math.toRadians(angle));

            double oX = this.x + size / 2;
            double oY = this.y + size / 4;

            double x = ((this.x - oX) * cos) - ((this.y - oY) * sin) + oX;
            double y = ((this.y - oY) * cos) + ((this.x - oX) * sin) + oY;
            quadBuf.vertex(x, y, 0).texture(TEX_QUAD.x, TEX_QUAD.y).color(color.r, color.g, color.b, color.a).next();

            x = ((this.x + size - oX) * cos) - ((this.y - oY) * sin) + oX;
            y = ((this.y - oY) * cos) + ((this.x + size - oX) * sin) + oY;
            quadBuf.vertex(x, y, 0).texture(TEX_QUAD.x + TEX_QUAD.width, TEX_QUAD.y).color(color.r, color.g, color.b, color.a).next();

            x = ((this.x + size / 2 - oX) * cos) - ((this.y + size / 2 - oY) * sin) + oX;
            y = ((this.y + size / 2 - oY) * cos) + ((this.x + size / 2 - oX) * sin) + oY;
            quadBuf.vertex(x, y, 0).texture(TEX_QUAD.x + TEX_QUAD.width, TEX_QUAD.y + TEX_QUAD.height).color(color.r, color.g, color.b, color.a).next();
            quadBuf.vertex(x, y, 0).texture(TEX_QUAD.x, TEX_QUAD.y + TEX_QUAD.height).color(color.r, color.g, color.b, color.a).next();
        }

        @Override
        void free() {
            triangleOperationPool.free(this);
        }
    }

    private class LineOperation extends Operation {
        double x1, y1;
        double x2, y2;
        Color color;

        @Override
        void render() {
            lineBuf.vertex(x1, y1, 0).color(color.r, color.g, color.b, color.a).next();
            lineBuf.vertex(x2, y2, 0).color(color.r, color.g, color.b, color.a).next();
        }

        @Override
        void free() {
            lineOperationPool.free(this);
        }
    }

    private class ScissorOperation extends Operation {
        double x, y;
        double width, height;

        boolean start;
        boolean resetQuadLine;
        boolean preScissorTest;

        @Override
        void render() {
            if (resetQuadLine) {
                endBuffers();
                begin();
            }

            if (start) {
                preScissorTest = GuiRenderer.this.preScissorTest;
                GuiRenderer.this.preScissorTest = true;

                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor((int) x, (int) y, (int) width, (int) height);
            } else {
                if (!preScissorTest) GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GuiRenderer.this.preScissorTest = preScissorTest;
            }
        }

        @Override
        void free() {
            scissorOperationPool.free(this);
        }
    }

    private class ItemOperation extends Operation {
        double x, y;
        ItemStack itemStack;

        @Override
        void render() {
            GlStateManager.enableTexture();
            DiffuseLighting.enableForItems();
            GlStateManager.enableDepthTest();
            MinecraftClient.getInstance().getItemRenderer().renderGuiItem(itemStack, (int) x, (int) y);
        }

        @Override
        void free() {
            itemOperationPool.free(this);
        }
    }

    private class TextOperation extends Operation {
        String text;
        double x, y;
        Color color;
        boolean shadow;

        @Override
        void render() {
            if (shadow) Utils.drawTextWithShadow(text, (float) x, (float) y, color.getPacked());
            else Utils.drawText(text, (float) x, (float) y, color.getPacked());
        }

        @Override
        void free() {
            textOperationPool.free(this);
        }
    }

    private class TextScissorOperation extends TextOperation {
        double width, height;

        boolean start;
        boolean preTextScissorTest;

        @Override
        void render() {
            if (start) {
                preTextScissorTest = GuiRenderer.this.preTextScissorTest;
                GuiRenderer.this.preTextScissorTest = true;

                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor((int) x, (int) y, (int) width, (int) height);
            } else {
                if (!preTextScissorTest) GL11.glDisable(GL11.GL_SCISSOR_TEST);
                else {
                    TextScissorOperation op = null;
                    for (int i = textOperationI - 1; i >= 0; i--) {
                        TextOperation textOperation = textOperations.get(i);
                        if (textOperation instanceof TextScissorOperation && !textOperation.equals(this) && ((TextScissorOperation) textOperation).start) {
                            op = (TextScissorOperation) textOperation;
                            break;
                        }
                    }

                    if (op != null) {
                        GL11.glScissor((int) op.x, (int) op.y, (int) op.width, (int) op.height);
                    }
                }
                GuiRenderer.this.preTextScissorTest = preTextScissorTest;
            }
        }

        @Override
        void free() {
            textScissorOperationPool.free(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TextScissorOperation operation = (TextScissorOperation) o;
            return Double.compare(operation.x, x) == 0 &&
                    Double.compare(operation.y, y) == 0 &&
                    Double.compare(operation.width, width) == 0 &&
                    Double.compare(operation.height, height) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, width, height);
        }
    }
}
