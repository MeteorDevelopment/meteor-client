package minegame159.meteorclient.gui.clickgui;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.widgets.*;
import minegame159.meteorclient.utils.ProfileUtils;

import java.util.List;

public class WProfiles extends WWindow {
    public WProfiles() {
        super("Profiles", Config.WindowType.Profiles, 4, 4, false);

        onDragged = panel -> {
            Config.WindowConfig winConfig = Config.INSTANCE.getWindowConfig(getType(), false);
            winConfig.setPos(panel.boundingBox.x, panel.boundingBox.y);
        };

        initWidgets();
    }

    private void initWidgets() {
        WGrid grid = add(new WGrid(4, 4, 4));

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
        if (profiles.size() > 0) add(new WHorizontalSeparator());
        WHorizontalList hList = add(new WHorizontalList(4));
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
