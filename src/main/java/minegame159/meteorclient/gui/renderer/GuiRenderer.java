package minegame159.meteorclient.gui.renderer;

import minegame159.meteorclient.MeteorClient;
import com.mojang.blaze3d.systems.RenderSystem;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.widgets.Cell;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.rendering.Renderer;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Pool;
import minegame159.meteorclient.utils.TextureRegion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class GuiRenderer {
    public static final GuiRenderer INSTANCE = new GuiRenderer();

    private static final Color DEBUG_COLOR_WIDGET = new Color(25, 25, 225);
    private static final Color DEBUG_COLOR_CELL = new Color(25, 225, 25);

    // Static texture stuff
    private static final Identifier TEXTURE = new Identifier("meteor-client", "gui.png");
    private static final int TEXTURE_WIDTH = 97;
    private static final int TEXTURE_HEIGHT = 32;

    public static final TextureRegion TEX_QUAD = new TextureRegion(TEXTURE_WIDTH, TEXTURE_HEIGHT, 0, 0, 1, 1, null, null, null);
    public static final TextureRegion TEX_RESET = new TextureRegion(TEXTURE_WIDTH, TEXTURE_HEIGHT, 1, 0, 32, 32, GuiConfig.INSTANCE.reset, GuiConfig.INSTANCE.resetHovered, GuiConfig.INSTANCE.resetPressed);
    public static final TextureRegion TEX_SLIDER_HANDLE = new TextureRegion(TEXTURE_WIDTH, TEXTURE_HEIGHT, 33, 0, 32, 32, GuiConfig.INSTANCE.sliderHandle, GuiConfig.INSTANCE.sliderHandleHovered, GuiConfig.INSTANCE.sliderHandlePressed);
    public static final TextureRegion TEX_EDIT = new TextureRegion(TEXTURE_WIDTH, TEXTURE_HEIGHT, 65, 0, 32, 32, GuiConfig.INSTANCE.edit, GuiConfig.INSTANCE.editHovered, GuiConfig.INSTANCE.editPressed);

    // Operation pools
    final Pool<QuadOperation> quadOperationPool = new Pool<>(QuadOperation::new);
    final Pool<TriangleOperation> triangleOperationPool = new Pool<>(TriangleOperation::new);
    final Pool<LineOperation> lineOperationPool = new Pool<>(LineOperation::new);
    final Pool<ItemOperation> itemOperationPool = new Pool<>(ItemOperation::new);
    final Pool<TextOperation> textOperationPool = new Pool<>(TextOperation::new);

    // Scissor stuff
    final Pool<Scissor> scissorPool = new Pool<>(Scissor::new);
    private Scissor scissor, currentScissor;

    private Operation tooltip;

    // Begin and End
    void beginBuffers() {
        if (!Renderer.isBuilding()) {
            Renderer.beginGui();
            MeteorClient.FONT.begin();
        }
    }

    public void begin() {
        MinecraftClient mc = MinecraftClient.getInstance();
        scissor = currentScissor = scissorPool.get().set(null, 0, 0, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight(), false, false);
    }

    void endBuffers() {
        if (Renderer.isBuilding()) {
            MinecraftClient.getInstance().getTextureManager().bindTexture(TEXTURE);
            Renderer.end(true);
            MeteorClient.FONT.end();
        }
    }

    public void end() {
        scissor.render(this);
        scissorPool.free(scissor);
        scissor = null;

        if (tooltip != null) {
            tooltip.render(this);
            tooltip.free(this);
            tooltip = null;
        }
    }

    // Scissor
    public void beginScissor(double x, double y, double width, double height, boolean textOnly) {
        Scissor newScissor = scissorPool.get().set(currentScissor, x, y, width, height, true, textOnly);
        currentScissor.scissorStack.add(newScissor);
        currentScissor = newScissor;
    }

    public void endScissor() {
        currentScissor = currentScissor.parent;
    }

    public void beginScissor(WWidget widget, double padTop, double padRight, double padBottom, double padLeft, boolean textOnly) {
        double x = widget.x + padLeft;
        double y = MinecraftClient.getInstance().getWindow().getScaledHeight() - widget.y - widget.height + padTop;
        double width = widget.width - padLeft - padRight;
        double height = widget.height - padTop - padBottom;

        beginScissor(x, y, width, height, textOnly);
    }

    // Quad
    public void renderQuad(double x, double y, double width, double height, TextureRegion tex, Color color1, Color color2, Color color3, Color color4) {
        currentScissor.operations.add(quadOperationPool.get().set(x, y, width, height, tex, color1, color2, color3, color4));
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

    // Triangle
    public void renderTriangle(double x, double y, double size, double angle, Color color) {
        currentScissor.operations.add(triangleOperationPool.get().set(x, y, size, angle, color));
    }

    // Line
    public void renderLine(double x1, double y1, double x2, double y2, Color color) {
        currentScissor.operations.add(lineOperationPool.get().set(x1, y1, x2, y2, color));
    }

    // Item
    public void renderItem(double x, double y, ItemStack itemStack) {
        currentScissor.postOperations.add(itemOperationPool.get().set(x, y, itemStack));
    }

    // Text
    public void renderText(String text, double x, double y, Color color, boolean shadow) {
        currentScissor.postOperations.add(textOperationPool.get().set(text, x, y, color, shadow));
    }

    public void renderTooltip(String text, double x, double y, Color color) {
        tooltip = textOperationPool.get().set(text, x, y, color, true);
    }

    // Debug
    private void renderDebug(WWidget widget, boolean begin) {
        if (begin) begin();

        renderLine(widget.x, widget.y, widget.x + widget.width, widget.y, DEBUG_COLOR_WIDGET);
        renderLine(widget.x, widget.y + widget.height, widget.x + widget.width, widget.y + widget.height, DEBUG_COLOR_WIDGET);
        renderLine(widget.x, widget.y, widget.x, widget.y + widget.height, DEBUG_COLOR_WIDGET);
        renderLine(widget.x + widget.width, widget.y, widget.x + widget.width, widget.y + widget.height, DEBUG_COLOR_WIDGET);

        for (Cell<?> cell : widget.getCells()) {
            renderLine(cell.getX(), cell.getY(), cell.getX() + cell.getWidth(), cell.getY(), DEBUG_COLOR_CELL);
            renderLine(cell.getX(), cell.getY() + cell.getHeight(), cell.getX() + cell.getWidth(), cell.getY() + cell.getHeight(), DEBUG_COLOR_CELL);
            renderLine(cell.getX(), cell.getY(), cell.getX(), cell.getY() + cell.getHeight(), DEBUG_COLOR_CELL);
            renderLine(cell.getX() + cell.getWidth(), cell.getY(), cell.getX() + cell.getWidth(), cell.getY() + cell.getHeight(), DEBUG_COLOR_CELL);

            renderDebug(cell.getWidget(), false);
        }

        if (begin) end();
    }

    public void renderDebug(WWidget widget) {
        renderDebug(widget, true);
    }
}
