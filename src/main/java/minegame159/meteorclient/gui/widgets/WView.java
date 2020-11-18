package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.renderer.GuiRenderer;
import minegame159.meteorclient.utils.Utils;
import net.minecraft.client.MinecraftClient;

public class WView extends WTable {
    public double maxHeight;

    private boolean hasScrollBar;
    private double actualHeight;

    private final boolean onlyWhenMouseOver;
    private double scrollHeight, lastScrollHeight;

    public WView(boolean onlyWhenMouseOver) {
        this.onlyWhenMouseOver = onlyWhenMouseOver;

        maxHeight = MinecraftClient.getInstance().getWindow().getFramebufferHeight() - 128;
        pad(0);
    }

    public WView() {
        this(false);
    }

    @Override
    protected void onCalculateSize(GuiRenderer renderer) {
        super.onCalculateSize(renderer);

        recalculateScroll();
    }

    private void recalculateScroll() {
        boolean hadScrollBar = hasScrollBar;

        if (height > maxHeight) {
            hasScrollBar = true;
            actualHeight = height;
            height = maxHeight;

            if (hadScrollBar) {
                moveWidgets();
            } else {
                scrollHeight = 0;
                lastScrollHeight = 0;
            }
        } else {
            if (hadScrollBar) moveWidgets(scrollHeight);
            hasScrollBar = false;
            actualHeight = height;
        }
    }

    @Override
    protected boolean onMouseScrolled(double amount) {
        if (hasScrollBar && (!onlyWhenMouseOver || mouseOver)) {
            scrollHeight -= amount * 22 * GuiConfig.INSTANCE.scrollSensitivity;
            moveWidgets();
            return true;
        }

        return false;
    }

    public double changeHeight(double delta) {
        double preHeight = height;
        height = actualHeight + delta;
        recalculateScroll();
        return preHeight - height;
    }

    public void moveWidgets() {
        scrollHeight = Utils.clamp(scrollHeight, 0, actualHeight - height);

        double deltaY = -(scrollHeight - lastScrollHeight);
        lastScrollHeight = scrollHeight;

        moveWidgets(deltaY);
    }

    private void moveWidgets(double deltaY) {
        for (Cell<?> cell : getCells()) move(cell.getWidget(), 0, deltaY, false);
        mouseMoved(MinecraftClient.getInstance().mouse.getX(), MinecraftClient.getInstance().mouse.getY());
    }

    @Override
    public void render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!visible) return;

        boolean scissor = hasScrollBar;
        if (scissor) renderer.beginScissor(x, y, width, height);
        super.render(renderer, mouseX, mouseY, delta);
        if (scissor) renderer.endScissor();
    }
}
