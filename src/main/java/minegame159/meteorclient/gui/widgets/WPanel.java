package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.modules.setting.GUI;

public class WPanel extends WWidget {
    public WPanel() {
        boundingBox.autoSize = true;
    }

    @Override
    public void onRender(double delta) {
        renderBackground(GUI.background, GUI.outline);
    }
}
