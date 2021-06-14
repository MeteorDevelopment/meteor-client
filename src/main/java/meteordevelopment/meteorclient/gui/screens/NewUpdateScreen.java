/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import com.g00fy2.versioncompare.Version;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.systems.config.Config;
import net.minecraft.util.Util;

public class NewUpdateScreen extends WindowScreen {
    public NewUpdateScreen(GuiTheme theme, Version latestVer) {
        super(theme, "New Update");

        add(theme.label("A new version of Meteor has been released."));

        add(theme.horizontalSeparator()).expandX();

        WTable versionsT = add(theme.table()).widget();
        versionsT.add(theme.label("Your version:"));
        versionsT.add(theme.label(Config.get().version.getOriginalString()));
        versionsT.row();
        versionsT.add(theme.label("Latest version"));
        versionsT.add(theme.label(latestVer.getOriginalString()));

        add(theme.horizontalSeparator()).expandX();

        WHorizontalList buttonsL = add(theme.horizontalList()).widget();
        buttonsL.add(theme.button("Download " + latestVer.getOriginalString())).expandX().widget().action = () -> Util.getOperatingSystem().open("http://meteorclient.com/");
        buttonsL.add(theme.button("OK")).expandX().widget().action = this::onClose;
    }
}
