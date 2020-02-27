package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.Vector2;

public class WQuad extends WWidget {
    public Color color;

    public WQuad(Color color) {
        this.color = color;
    }

    @Override
    public Vector2 calculateCustomSize() {
        return new Vector2(Utils.getTextHeight() + 6, Utils.getTextHeight() + 6);
    }

    @Override
    public void onRender(double delta) {
        RenderUtils.quad(boundingBox.x, boundingBox.y, boundingBox.getWidth(), boundingBox.getHeight(), color);
    }
}
