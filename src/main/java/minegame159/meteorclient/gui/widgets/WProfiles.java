package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.utils.ProfileUtils;

import java.util.List;

public class WProfiles extends WWindow {
    public WProfiles() {
        super("Profiles", false, GuiConfig.WindowType.Profiles);

        onDragged = window -> {
            GuiConfig.WindowConfig winConfig = GuiConfig.INSTANCE.getWindowConfig(type, false);
            winConfig.setPos(x, y);
        };

        initWidgets();
    }

    private void initWidgets() {
        // Profiles
        WTable profilesList = add(new WTable()).getWidget();
        List<String> profiles = ProfileUtils.getProfiles();
        for (String profile : profiles) {
            profilesList.add(new WLabel(profile));

            WButton save = profilesList.add(new WButton("Save")).getWidget();
            save.action = button -> ProfileUtils.save(profile);

            WButton load = profilesList.add(new WButton("Load")).getWidget();
            load.action = button -> ProfileUtils.load(profile);

            WMinus delete = profilesList.add(new WMinus()).getWidget();
            delete.action = minus -> {
                ProfileUtils.delete(profile);
                clear();
                initWidgets();
            };

            profilesList.row();
        }
        row();

        // New profile
        if (profiles.size() > 0) {
            add(new WHorizontalSeparator()).fillX().expandX();
            row();
        }

        WTable newList = add(new WTable()).fillX().expandX().getWidget();
        WTextBox name = newList.add(new WTextBox("", 70)).fillX().expandX().getWidget();
        WPlus save = newList.add(new WPlus()).getWidget();
        save.action = plus -> {
            if (ProfileUtils.save(name.text)) {
                clear();
                initWidgets();
            }
        };
    }
}
