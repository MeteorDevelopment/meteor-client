package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.RenderUtils;
import org.lwjgl.opengl.GL11;

public class WDebugRenderer {
    private static final Color color = new Color(25, 25, 255);

    public static void render(WWidget widget, boolean beginEndLines) {
        GL11.glLineWidth(1);
        if (beginEndLines) RenderUtils.beginLines();
        line(widget.boundingBox.x, widget.boundingBox.y, widget.boundingBox.x + widget.boundingBox.getWidth(), widget.boundingBox.y);
        line(widget.boundingBox.x + widget.boundingBox.getWidth(), widget.boundingBox.y, widget.boundingBox.x + widget.boundingBox.getWidth(), widget.boundingBox.y + widget.boundingBox.getHeight());
        line(widget.boundingBox.x, widget.boundingBox.y + widget.boundingBox.getHeight(), widget.boundingBox.x + widget.boundingBox.getWidth(), widget.boundingBox.y + widget.boundingBox.getHeight());
        line(widget.boundingBox.x, widget.boundingBox.y, widget.boundingBox.x, widget.boundingBox.y + widget.boundingBox.getHeight());
        for (WWidget w : widget.widgets) render(w, false);
        if (beginEndLines) RenderUtils.endLines();
    }

    private static void line(double x1, double y1, double x2, double y2) {
        RenderUtils.line(x1, y1, 0, x2, y2, 0, color);
    }
}
