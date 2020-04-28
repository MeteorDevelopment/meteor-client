package minegame159.meteorclient.gui.screens.topbar;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.TopBarType;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WModuleGroup;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WWindowController;
import minegame159.meteorclient.modules.ModuleManager;

public class TopBarModules extends TopBarScreen {
    public TopBarModules() {
        super(TopBarType.Modules);

        add(new WWindowController());

        // Help text
        WTable bottomLeft = add(new WTable()).bottom().left().getWidget();
        bottomLeft.pad(4);
        bottomLeft.add(new WLabel("Left click - activate/deactivate module", true));
        bottomLeft.row();
        bottomLeft.add(new WLabel("Right click - open module settings", true));
    }

    @Override
    protected void init() {
        if (WModuleGroup.MOVED) Config.INSTANCE.save();
        WModuleGroup.MOVED = false;
    }

    @Override
    public void onClose() {
        ModuleManager.INSTANCE.save();
        if (WModuleGroup.MOVED) Config.INSTANCE.save();
        super.onClose();
    }
}
