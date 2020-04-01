package minegame159.meteorclient.gui.clickgui;

import minegame159.meteorclient.gui.Alignment;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WVerticalList;
import minegame159.meteorclient.modules.ModuleManager;

public class ClickGUI extends WidgetScreen {
    public ClickGUI() {
        super("ClickGUI");

        // Module Controller
        add(new WModuleController());

        // Help text
        WVerticalList helpList = add(new WVerticalList(4));
        helpList.boundingBox.setMargin(4);
        helpList.boundingBox.alignment.y = Alignment.Y.Bottom;
        helpList.add(new WLabel("Left click - activate/deactivate module", true));
        helpList.add(new WLabel("Right click - open module settings", true));

        layout();
    }

    @Override
    public void onClose() {
        ModuleManager.INSTANCE.save();
        super.onClose();
    }
}
