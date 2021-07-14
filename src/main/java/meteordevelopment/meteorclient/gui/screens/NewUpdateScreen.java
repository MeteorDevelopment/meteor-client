/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.misc.Version;
import net.minecraft.util.Util;

public class NewUpdateScreen extends WindowScreen {
    public NewUpdateScreen(GuiTheme theme, Version latestVer) {
        super(theme, "New Update");

        add(theme.label("A new version of Meteor has been released."));

        add(theme.horizontalSeparator()).expandX();

        WTable versionsT = add(theme.table()).widget();
        versionsT.add(theme.label("Your version:"));
        versionsT.add(theme.label(Config.get().version.toString()));
        versionsT.row();
        versionsT.add(theme.label("Latest version"));
        versionsT.add(theme.label(latestVer.toString()));

        add(theme.horizontalSeparator()).expandX();

        WHorizontalList buttonsL = add(theme.horizontalList()).widget();
        buttonsL.add(theme.button("Download " + latestVer)).expandX().widget().action = () -> Util.getOperatingSystem().open("https://meteorclient.com/");
        buttonsL.add(theme.button("OK")).expandX().widget().action = this::onClose;
    }
}
