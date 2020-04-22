package minegame159.meteorclient.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import minegame159.meteorclient.gui.widgets.Cell;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Pool;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GuiRenderer {
    private static final Color DEBUG_COLOR_WIDGET = new Color(25, 25, 225);
    private static final Color DEBUG_COLOR_CELL = new Color(25, 225, 25);

    private static Tessellator lineTesselator = new Tessellator(1000);
    private static BufferBuilder lineBuf = lineTesselator.getBuffer();

    private static Tessellator quadTesselator = new Tessellator(1000);
    private static BufferBuilder quadBuf = quadTesselator.getBuffer();

    private Pool<QuadOperation> quadOperationPool = new Pool<>(QuadOperation::new);
    private Pool<LineOperation> lineOperationPool = new Pool<>(LineOperation::new);
    private Pool<ItemOperation> itemOperationPool = new Pool<>(ItemOperation::new);
    private Pool<TextOperation> textOperationPool = new Pool<>(TextOperation::new);
    private Pool<ScissorOperation> scissorOperationPool = new Pool<>(ScissorOperation::new);
    private Pool<TextScissorOperation> textScissorOperationPool = new Pool<>(TextScissorOperation::new);

    private List<Operation> operations = new ArrayList<>();
    private List<Operation> postOperations = new ArrayList<>();
    private List<TextOperation> textOperations = new ArrayList<>();

    private boolean preScissorTest = false;
    private boolean preTextScissorTest = false;

    private int textOperationI;

    public void begin() {
        // Lines
        lineBuf.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);

        // Quads
        quadBuf.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
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

    public void renderQuad(double x, double y, double width, double height, Color color) {
        QuadOperation operation = quadOperationPool.get();
        operation.x = x;
        operation.y = y;
        operation.width = width;
        operation.height = height;
        operation.color = color;
        operations.add(operation);
    }

    public void renderItem(double x, double y, ItemStack itemStack) {
        ItemOperation operation = itemOperationPool.get();
        operation.x = x;
        operation.y = y;
        operation.itemStack = itemStack;
        postOperations.add(operation);
    }

    public void end() {
        GlStateManager.enableBlend();
        GlStateManager.disableCull();

        GlStateManager.pushMatrix();
        GL11.glLineWidth(1);

        preScissorTest = false;
        preTextScissorTest = false;

        // Render quads and lines
        GlStateManager.disableTexture();
        for (Operation operation : operations) {
            operation.render();
            operation.free();
        }
        operations.clear();
        quadTesselator.draw();
        lineTesselator.draw();

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
        TextOperation textOperation = textOperationPool.get();
        textOperation.text = text;
        textOperation.x = x;
        textOperation.y = y;
        textOperation.color = color;
        textOperation.shadow = shadow;
        textOperations.add(textOperation);
    }

    public void startTextScissor(WWidget widget, double padTop, double padRight, double padBottom, double padLeft) {
        TextScissorOperation operation = textScissorOperationPool.get();
        double scaleFactor = MinecraftClient.getInstance().window.getScaleFactor();
        operation.x = (widget.x + padLeft) * scaleFactor;
        operation.y = (MinecraftClient.getInstance().window.getScaledHeight() - widget.y - widget.height + padTop) * scaleFactor;
        operation.width = (widget.width - padLeft - padRight) * scaleFactor;
        operation.height = (widget.height - padTop - padBottom) * scaleFactor;
        operation.start = true;
        textOperations.add(operation);
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
        Color color;

        @Override
        void render() {
            quadBuf.vertex(x, y, 0).color(color.r, color.g, color.b, color.a).next();
            quadBuf.vertex(x + width, y, 0).color(color.r, color.g, color.b, color.a).next();
            quadBuf.vertex(x + width, y + height, 0).color(color.r, color.g, color.b, color.a).next();
            quadBuf.vertex(x, y + height, 0).color(color.r, color.g, color.b, color.a).next();
        }

        @Override
        void free() {
            quadOperationPool.free(this);
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
                quadTesselator.draw();
                lineTesselator.draw();
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
