package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.gui.WidgetScreen;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WWindowController;
import minegame159.meteorclient.gui.widgets.WTable;

public class ClickGUI extends WidgetScreen {
    public ClickGUI() {
        super("ClickGUI");

        add(new WWindowController());

        // Help text
        WTable bottomLeft = add(new WTable()).bottom().left().getWidget();
        bottomLeft.pad(4);
        bottomLeft.add(new WLabel("Left click - activate/deactivate module", true));
        bottomLeft.row();
        bottomLeft.add(new WLabel("Right click - open module settings", true));
    }

    @Override
    public void onClose() {
        ModuleManager.INSTANCE.save();
        super.onClose();
    }
}
