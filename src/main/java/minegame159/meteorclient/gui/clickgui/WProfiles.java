package minegame159.meteorclient.gui.clickgui;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.utils.ProfileUtils;
import minegame159.meteorclient.utils.Vector2;
import net.minecraft.client.MinecraftClient;

import java.util.List;

public class WProfiles extends WPanel {
    public WProfiles() {
        boundingBox.setMargin(6);

        onDragged = panel -> {
            Vector2 pos = Config.INSTANCE.getGuiPositionNotNull(Category.Profiles);
            pos.x = panel.boundingBox.x;
            pos.y = panel.boundingBox.y;
        };

        initWidgets();
    }

    private void initWidgets() {
        WVerticalList list = add(new WVerticalList(4));
        list.maxHeight = MinecraftClient.getInstance().window.getScaledHeight() - 32;

        list.add(new WLabel("Profiles", true));
        list.add(new WHorizontalSeparatorBigger());

        WGrid grid = list.add(new WGrid(4, 4, 4));

        // Profiles
        List<String> profiles = ProfileUtils.getProfiles();
        for (String profile : profiles) {
            WLabel name = new WLabel(profile);

            WButton save = new WButton("Save");
            save.action = () -> ProfileUtils.save(profile);

            WButton load = new WButton("Load");
            load.action = () -> ProfileUtils.load(profile);

            WMinus delete = new WMinus();
            delete.action = () -> {
                ProfileUtils.delete(profile);
                widgets.clear();
                initWidgets();
                layout();
            };

            grid.addRow(name, save, load, delete);
        }

        // New profile
        if (profiles.size() > 0) list.add(new WHorizontalSeparator());
        WHorizontalList hList = list.add(new WHorizontalList(4));
        WTextBox name = hList.add(new WTextBox("", 70));
        WPlus save = hList.add(new WPlus());
        save.action = () -> {
            if (ProfileUtils.save(name.text)) {
                widgets.clear();
                initWidgets();
                layout();
            }
        };
    }
}
