package minegame159.meteorclient.gui.clickgui;

import minegame159.meteorclient.gui.BoundingBox;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.Box;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Vector2;

public class WHorizontalSeparatorBigger extends WWidget {
    public WHorizontalSeparatorBigger() {
        boundingBox = new HackBoundingBox(this::calculateCustomSize);
    }

    @Override
    public Vector2 calculateCustomSize() {
        return new Vector2(0, 1);
    }

    @Override
    public void onRender(double delta) {
        RenderUtils.quad(parent.parent.boundingBox.x, boundingBox.y, parent.parent.boundingBox.getWidth(), boundingBox.innerHeight, GUI.outline);
    }

    private static class HackBoundingBox extends BoundingBox {
        public HackBoundingBox(CalculateCustomSize calculateCustomSize) {
            super(calculateCustomSize);
        }

        @Override
        public void calculatePos(Box box) {
            super.calculatePos(box);
            x -= 6;
        }
    }
}
