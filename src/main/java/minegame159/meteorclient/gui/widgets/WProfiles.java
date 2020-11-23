/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.gui.widgets;

import minegame159.meteorclient.gui.GuiConfig;
import minegame159.meteorclient.gui.screens.topbar.TopBarModules;
import minegame159.meteorclient.utils.ProfileUtils;

public class WProfiles extends WWindow {
    public WProfiles() {
        super("Profiles", GuiConfig.INSTANCE.getWindowConfig(GuiConfig.WindowType.Profiles).isExpanded(), true);
        type = GuiConfig.WindowType.Profiles;

        action = () -> {
            GuiConfig.INSTANCE.getWindowConfig(type).setPos(x, y);
            TopBarModules.MOVED = true;
        };

        initWidgets();
    }

    private void initWidgets() {
        // Profiles
        WTable profiles = add(new WTable()).getWidget();
        for (String profile : ProfileUtils.getProfiles()) {
            profiles.add(new WLabel(profile));

            WButton save = profiles.add(new WButton("Save")).getWidget();
            save.action = () -> ProfileUtils.save(profile);

            WButton load = profiles.add(new WButton("Load")).getWidget();
            load.action = () -> ProfileUtils.load(profile);

            WMinus delete = profiles.add(new WMinus()).getWidget();
            delete.action = () -> {
                ProfileUtils.delete(profile);
                clear();
                initWidgets();
            };

            profiles.row();
        }
        row();

        // New Profile
        if (profiles.getCells().size() > 0) {
            add(new WHorizontalSeparator());
            row();
        }

        WTable t = add(new WTable()).fillX().expandX().getWidget();
        WTextBox name = t.add(new WTextBox("", 140)).fillX().expandX().getWidget();
        WPlus add = t.add(new WPlus()).getWidget();
        add.action = () -> {
            if (ProfileUtils.save(name.getText())) {
                clear();
                initWidgets();
            }
        };
    }
}
