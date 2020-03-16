package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.Color;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.Vector2;

public class WLabel extends WWidget {
    public String text;
    public boolean shadow;
    public Color color = GUI.text;

    public WLabel(String text, boolean shadow) {
        this.text = text;
        this.shadow = shadow;
    }

    public WLabel(String text) {
        this(text, false);
    }

    @Override
    public Vector2 calculateCustomSize() {
        return new Vector2(Utils.getTextWidth(text), Utils.getTextHeight());
    }

    @Override
    public void onRenderPost(double delta) {
        if (shadow) Utils.drawTextWithShadow(text, (int) (boundingBox.getInnerX() + 0.5), (int) (boundingBox.getInnerY() + 0.5), GUI.textC);
        else Utils.drawText(text, (int) (boundingBox.getInnerX() + 0.5), (int) (boundingBox.getInnerY() + 0.5), color.getPacked());
    }
}
