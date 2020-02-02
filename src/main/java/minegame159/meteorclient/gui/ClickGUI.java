package minegame159.meteorclient.gui;

import minegame159.meteorclient.gui.widgets.Label;
import minegame159.meteorclient.gui.widgets.ModuleGroup;
import minegame159.meteorclient.gui.widgets.ModuleGroupController;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.Vector2;
import net.minecraft.client.MinecraftClient;

public class ClickGUI extends WidgetScreen {
    private ModuleGroupController groups;
    private Label helpLabel1, helpLabel2;

    public ClickGUI() {
        super("ClickGUI");
        groups = new ModuleGroupController();

        double x = 10;
        double y = 10;

        for (Category category : ModuleManager.getCategories()) {
            ModuleGroup group = groups.addGroup(category, new Vector2(x, y));

            x = group.x + group.widthMargin() + 10;
        }

        addWidget(groups);

        helpLabel1 = new Label(0, "Left click: toggle", true);
        addWidget(helpLabel1);
        helpLabel2 = new Label(0, "Right click: open settings", true);
        addWidget(helpLabel2);
        setHelpPos();
    }

    private void setHelpPos() {
        double y = MinecraftClient.getInstance().getWindow().getScaledHeight();

        helpLabel2.x = 2;
        helpLabel2.y = y - Utils.getTextHeight() - 2;

        y = helpLabel2.y;

        helpLabel1.x = 2;
        helpLabel1.y = y - Utils.getTextHeight() - 2;
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        setHelpPos();

        super.resize(client, width, height);
    }
}
