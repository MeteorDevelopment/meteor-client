package minegame159.meteorclient.gui;

import minegame159.meteorclient.gui.clickgui.WHorizontalSeparatorBigger;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WPanel;
import minegame159.meteorclient.gui.widgets.WVerticalList;
import minegame159.meteorclient.gui.widgets.WWidget;
import net.minecraft.client.MinecraftClient;

public class PanelListScreen extends WidgetScreen {
    private WPanel panel;
    private WVerticalList list;

    public PanelListScreen(String title) {
        super(title);

        // Panel
        panel = super.add(new WPanel());
        panel.boundingBox.setMargin(6);
        panel.boundingBox.alignment.set(Alignment.X.Center, Alignment.Y.Center);

        // Vertical List
        list = panel.add(new WVerticalList(4));
        list.maxHeight = MinecraftClient.getInstance().window.getScaledHeight() - 32;

        // Title
        list.add(new WLabel(title, true)).boundingBox.alignment.x = Alignment.X.Center;
        list.add(new WHorizontalSeparatorBigger());
    }

    @Override
    public <T extends WWidget> T add(T widget) {
        return list.add(widget);
    }

    @Override
    public void clear() {
        list.widgets.clear();

        // Title
        list.add(new WLabel(title.asString(), true)).boundingBox.alignment.x = Alignment.X.Center;
        list.add(new WHorizontalSeparatorBigger());
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        list.maxHeight = height - 32;
        super.resize(client, width, height);
    }
}
