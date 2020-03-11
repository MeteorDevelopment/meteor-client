package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.WidgetLayout;
import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.Box;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.Vector2;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;

public class WVerticalList extends WWidget {
    public double maxHeight = -1;
    public boolean scrollOnlyWhenMouseOver = true;

    private boolean scrollingActive;
    private double scroll;
    private double maxScroll;

    public WVerticalList(double spacing) {
        boundingBox.autoSize = true;
        layout = new VerticalListLayout(spacing);
    }

    @Override
    public boolean onMouseScrolled(double amount) {
        if ((!scrollOnlyWhenMouseOver || mouseOver) && scrollingActive) {
            double preScroll = scroll;
            scroll -= amount * 8 * GUI.scrollMultiplier;
            scroll = Utils.clamp(scroll, 0, maxScroll);
            double deltaScroll = scroll - preScroll;

            for (WWidget widget : widgets) {
                move(widget, -deltaScroll);
                widget.mouseMove(MinecraftClient.getInstance().mouse.getX() / MinecraftClient.getInstance().window.getScaleFactor(), MinecraftClient.getInstance().mouse.getY() / MinecraftClient.getInstance().window.getScaleFactor());
            }
            return true;
        }

        return false;
    }

    private void move(WWidget widget, double deltaY) {
        widget.boundingBox.y += deltaY;

        for (WWidget w : widget.widgets) move(w, deltaY);
    }

    private void beginRender(boolean resetQuads) {
        if (scrollingActive) {
            if (resetQuads) {
                RenderUtils.endLines();
                RenderUtils.endQuads();
                RenderUtils.beginLines();
                RenderUtils.beginQuads();
            }
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            double scaleFactor = MinecraftClient.getInstance().window.getScaleFactor();
            GL11.glScissor((int) (boundingBox.getInnerX() * scaleFactor), (int) ((MinecraftClient.getInstance().window.getScaledHeight() - boundingBox.getInnerY() - boundingBox.innerHeight) * scaleFactor), (int) (boundingBox.innerWidth * scaleFactor), (int) (boundingBox.innerHeight * scaleFactor));
        }
    }

    private void endRender(boolean resetQuads) {
        if (scrollingActive) {
            if (resetQuads) {
                RenderUtils.endLines();
                RenderUtils.endQuads();
                RenderUtils.beginLines();
                RenderUtils.beginQuads();
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    @Override
    public void render(double delta) {
        beginRender(true);
        super.render(delta);
        endRender(true);
    }

    @Override
    public void renderPost(double delta, double mouseX, double mouseY) {
        beginRender(false);
        super.renderPost(delta, mouseX, mouseY);
        endRender(false);
    }

    public static class VerticalListLayout extends WidgetLayout {
        private double spacing;

        private int i;
        private double lastChildHeight;
        private Box box = new Box();

        public VerticalListLayout(double spacing) {
            this.spacing = spacing;
        }

        @Override
        public void reset(WWidget widget) {
            i = 0;
            box.x = widget.boundingBox.getInnerX();
            box.y = widget.boundingBox.getInnerY();
        }

        @Override
        public Vector2 calculateAutoSize(WWidget widget) {
            WVerticalList list = (WVerticalList) widget;
            Vector2 size = new Vector2();

            int i = 0;
            for (WWidget w : widget.widgets) {
                size.x = Math.max(size.x, w.boundingBox.getWidth());
                size.y += w.boundingBox.getHeight();
                if (i > 0) size.y += spacing;
                i++;
            }

            if (list.maxHeight >= 0) {
                list.scrollingActive = (size.y + list.boundingBox.marginTop + list.boundingBox.marginBottom) > list.maxHeight;
                if (list.scrollingActive) {
                    list.scroll = 0;
                    list.maxScroll = (size.y + list.boundingBox.marginTop + list.boundingBox.marginBottom) - list.maxHeight;
                    size.y = list.maxHeight - list.boundingBox.marginTop - list.boundingBox.marginBottom;
                }
            } else list.scrollingActive = false;

            return size;
        }

        @Override
        public Box layoutWidget(WWidget widget, WWidget child) {
            if (i > 0) box.y += spacing + lastChildHeight;

            box.width = widget.boundingBox.innerWidth;
            box.height = child.boundingBox.getHeight();

            lastChildHeight = child.boundingBox.getHeight();
            i++;
            return box;
        }
    }
}
