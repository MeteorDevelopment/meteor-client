package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.Vector2;

public class WHorizontalSeparator extends WWidget {
    private String text;

    public WHorizontalSeparator(String text) {
        boundingBox.calculateAutoSizePost = true;

        this.text = text;
    }

    public WHorizontalSeparator() {
        this(null);
    }

    @Override
    public Vector2 calculateCustomSize() {
        return new Vector2(Math.max(parent.boundingBox.innerWidth, 4 + Utils.getTextWidth(text) + 4), text == null ? 1 : Utils.getTextHeight());
    }

    @Override
    public void onRender(double delta) {
        int textWidth = Utils.getTextWidth(text);
        double textStart = boundingBox.innerWidth / 2.0 - textWidth / 2.0 - 2;
        double textEnd = 2 + textStart + textWidth + 2;

        double offsetY = boundingBox.innerHeight / 2.0 - 0.5;

        if (textWidth > 0) {
            RenderUtils.quad(boundingBox.getInnerX(), boundingBox.getInnerY() + offsetY, textStart, 1, GUI.separator);
            RenderUtils.quad(boundingBox.getInnerX() + textEnd, boundingBox.getInnerY() + offsetY, boundingBox.innerWidth - textEnd, 1, GUI.separator);
        } else {
            RenderUtils.quad(boundingBox.getInnerX(), boundingBox.getInnerY(), boundingBox.innerWidth, 1, GUI.separator);
        }
    }

    @Override
    public void onRenderPost(double delta) {
        double offset = boundingBox.innerWidth / 2.0 - Utils.getTextWidth(text) / 2.0;
        Utils.drawText(text, (float) (boundingBox.getInnerX() + offset), (float) boundingBox.getInnerY(), GUI.separatorC);
    }
}
