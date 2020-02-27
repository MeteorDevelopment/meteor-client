package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.setting.GUI;
import minegame159.meteorclient.utils.RenderUtils;
import minegame159.meteorclient.utils.Vector2;

public class WHorizontalSeparator extends WWidget {
    public WHorizontalSeparator() {
        boundingBox.calculateAutoSizePost = true;
    }

    @Override
    public Vector2 calculateCustomSize() {
        return new Vector2(parent.boundingBox.innerWidth, 1);
    }

    @Override
    public void onRender(double delta) {
        RenderUtils.quad(boundingBox.getInnerX(), boundingBox.getInnerY(), boundingBox.innerWidth, boundingBox.innerHeight, GUI.separator);
    }
}
