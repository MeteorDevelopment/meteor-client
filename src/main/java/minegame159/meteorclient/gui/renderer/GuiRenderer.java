package minegame159.meteorclient.gui.renderer;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.rendering.Fonts;
import minegame159.meteorclient.rendering.MeshBuilder;
import minegame159.meteorclient.rendering.MyFont;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Pool;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class GuiRenderer {
    private static final Color WHITE = new Color(255, 255, 255);
    private static final Identifier TEXTURE = new Identifier("meteor-client", "newgui.png");

    private final MeshBuilder mb = new MeshBuilder();

    private final Stack<Scissor> scissorStack = new ObjectArrayList<>();
    private final Pool<Scissor> scissorPool = new Pool<>(Scissor::new);

    private final List<Text> texts = new ArrayList<>();
    private final Pool<Text> textPool = new Pool<>(Text::new);

    public String tooltip;

    public void begin(boolean root) {
        mb.begin(GL11.GL_TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE);

        if (root) {
            Window window = MinecraftClient.getInstance().getWindow();
            beginScissor(0, 0, window.getFramebufferWidth(), window.getFramebufferHeight(), false);
        }
    }
    public void begin() {
        begin(false);
    }

    public void end(boolean root) {
        double mouseX = MinecraftClient.getInstance().mouse.getX();
        double mouseY = MinecraftClient.getInstance().mouse.getY();
        double tooltipWidth = tooltip != null ? textWidth(tooltip) : 0;

        if (root && tooltipWidth > 0) {
            quad(Region.FULL, mouseX + 8, mouseY + 8, tooltipWidth + 8, textHeight() + 8, GuiConfig.INSTANCE.background);
        }

        MinecraftClient.getInstance().getTextureManager().bindTexture(TEXTURE);
        mb.end(true);

        Fonts.get().begin();
        if (root && tooltipWidth > 0) {
            text(tooltip, mouseX + 8 + 4, mouseY + 8 + 4, false, GuiConfig.INSTANCE.text);
            tooltip = null;
        }
        for (Text text : texts) {
            text.render();
            textPool.free(text);
        }
        texts.clear();
        Fonts.get().end();

        if (root) endScissor();
    }
    public void end() {
        end(false);
    }

    public void beginScissor(double x, double y, double width, double height, boolean changeGlState) {
        if (!scissorStack.isEmpty()) {
            Scissor parent = scissorStack.top();

            if (x < parent.x) x = parent.x;
            else if (x + width > parent.x + parent.width) width -= (x + width) - (parent.x + parent.width);

            if (y < parent.y) y = parent.y;
            else if (y + height > parent.y + parent.height) height -= (y + height) - (parent.y + parent.height);
        }

        Scissor scissor = scissorPool.get().set(x, y, width, height, changeGlState);
        scissorStack.push(scissor);

        if (changeGlState) {
            end();
            begin();

            glEnable(GL_SCISSOR_TEST);
            scissor.apply();
        }
    }
    public void beginScissor(double x, double y, double width, double height) {
        beginScissor(x, y, width, height, true);
    }

    public void endScissor() {
        Scissor scissor = scissorStack.pop();

        if (scissor.changeGlState) {
            end();
            for (Runnable task : scissor.postTasks) task.run();

            if (scissorStack.isEmpty() || !scissorStack.top().changeGlState) {
                glDisable(GL_SCISSOR_TEST);
            } else {
                scissorStack.top().apply();
            }
            begin();
        } else {
            for (Runnable task : scissor.postTasks) task.run();
        }
    }

    public void quad(Region region, double x, double y, double width, double height, Color color1, Color color2, Color color3, Color color4) {
        mb.pos(x, y, 0).color(color1).texture(region.x, region.y).endVertex();
        mb.pos(x + width, y, 0).color(color2).texture(region.x + region.width, region.y).endVertex();
        mb.pos(x + width, y + height, 0).color(color3).texture(region.x + region.width, region.y + region.height).endVertex();

        mb.pos(x, y, 0).color(color1).texture(region.x, region.y).endVertex();
        mb.pos(x + width, y + height, 0).color(color3).texture(region.x + region.width, region.y + region.height).endVertex();
        mb.pos(x, y + height, 0).color(color4).texture(region.x, region.y + region.height).endVertex();
    }
    public void quad(Region region, double x, double y, double width, double height, Color color) {
        quad(region, x, y, width, height, color, color, color, color);
    }

    public void background(WWidget widget, boolean hovered, boolean pressed) {
        Color background = GuiConfig.INSTANCE.background;
        Color outline = GuiConfig.INSTANCE.outline;

        if (pressed) {
            background = GuiConfig.INSTANCE.backgroundPressed;
            outline = GuiConfig.INSTANCE.outlinePressed;
        } else if (hovered) {
            background = GuiConfig.INSTANCE.backgroundHovered;
            outline = GuiConfig.INSTANCE.outlineHovered;
        }

        quad(Region.FULL, widget.x, widget.y, widget.width, widget.height, background);
        quad(Region.FULL, widget.x, widget.y, widget.width, 2, outline);
        quad(Region.FULL, widget.x, widget.y + widget.height - 2, widget.width, 2, outline);
        quad(Region.FULL, widget.x, widget.y + 2, 2, widget.height - 4, outline);
        quad(Region.FULL, widget.x + widget.width - 2, widget.y + 2, 2, widget.height - 4, outline);
    }
    public void background(WWidget widget, boolean pressed) {
        background(widget, widget.mouseOver, pressed);
    }
    
    public void triangle(double x, double y, double size, double rotation, Color color) {
        double rad = Math.toRadians(rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double oX = x + size / 2;
        double oY = y + size / 4;

        double _x = ((x - oX) * cos) - ((y - oY) * sin) + oX;
        double _y = ((y - oY) * cos) + ((x - oX) * sin) + oY;
        mb.pos(_x, _y, 0).color(color).texture(Region.FULL.x, Region.FULL.y).endVertex();

        _x = ((x + size - oX) * cos) - ((y - oY) * sin) + oX;
        _y = ((y - oY) * cos) + ((x + size - oX) * sin) + oY;
        mb.pos(_x, _y, 0).color(color).texture(Region.FULL.x + Region.FULL.width, Region.FULL.y).endVertex();

        double v = y + size / 2 - oY;
        _x = ((x + size / 2 - oX) * cos) - (v * sin) + oX;
        _y = (v * cos) + ((x + size / 2 - oX) * sin) + oY;
        mb.pos(_x, _y, 0).color(color).texture(Region.FULL.x + Region.FULL.width, Region.FULL.y + Region.FULL.height).endVertex();
    }

    public void text(String text, double x, double y, boolean shadow, Color color) {
        texts.add(textPool.get().set(text, x, y, shadow, color, false));
    }
    public double textWidth(String text, int length) {
        return Fonts.get().getWidth(text, length);
    }
    public double textWidth(String text) {
        return Fonts.get().getWidth(text);
    }
    public double textHeight() {
        return Fonts.get().getHeight();
    }

    public void title(String text, double x, double y, Color color) {
        texts.add(textPool.get().set(text, x, y, false, color, true));
    }
    public double titleWidth(String text) {
        return Fonts.getTitle().getWidth(text);
    }
    public double titleHeight() {
        return Fonts.getTitle().getHeight();
    }

    public void post(Runnable task) {
        scissorStack.top().postTasks.add(task);
    }

    public void texture(double x, double y, double width, double height, double rotation, AbstractTexture texture) {
        post(() -> {
            mb.begin(GL_TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE);

            mb.pos(x, y, 0).color(WHITE).texture(0, 0).endVertex();
            mb.pos(x + width, y, 0).color(WHITE).texture(1, 0).endVertex();
            mb.pos(x + width, y + height, 0).color(WHITE).texture(1, 1).endVertex();
            mb.pos(x, y, 0).color(WHITE).texture(0, 0).endVertex();
            mb.pos(x + width, y + height, 0).color(WHITE).texture(1, 1).endVertex();
            mb.pos(x, y + height, 0).color(WHITE).texture(0, 1).endVertex();

            texture.bindTexture();
            GL11.glPushMatrix();
            GL11.glTranslated(x + width / 2, y + height / 2, 0);
            GL11.glRotated(rotation, 0, 0, 1);
            GL11.glTranslated(-x - width / 2, -y - height / 2, 0);
            mb.end(true);
            GL11.glPopMatrix();
        });
    }

    private static class Scissor {
        public int x, y;
        public int width, height;

        public boolean changeGlState;
        public List<Runnable> postTasks = new ArrayList<>();

        public Scissor set(double x, double y, double width, double height, boolean changeGlState) {
            this.x = (int) Math.round(x);
            this.y = (int) Math.round(y);
            this.width = (int) Math.round(width);
            this.height = (int) Math.round(height);
            this.changeGlState = changeGlState;

            if (this.width < 0) this.width = 0;
            if (this.height < 0) this.height = 0;

            postTasks.clear();

            return this;
        }

        public void apply() {
            glScissor(x, MinecraftClient.getInstance().getWindow().getFramebufferHeight() - y - height, width, height);
        }
    }

    private static class Text {
        public String text;
        public double x, y;
        public boolean shadow;
        public Color color;
        private boolean title;

        public Text set(String text, double x, double y, boolean shadow, Color color, boolean title) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.shadow = shadow;
            this.color = color;
            this.title = title;

            return this;
        }

        public void render() {
            MyFont font = title ? Fonts.getTitle() : Fonts.get();
            font.render(text, x, y, color);
        }
    }
}
