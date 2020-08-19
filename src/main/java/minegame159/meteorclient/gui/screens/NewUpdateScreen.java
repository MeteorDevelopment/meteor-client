package minegame159.meteorclient.gui.screens;

import minegame159.meteorclient.Config;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WHorizontalSeparator;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.utils.Version;
import net.minecraft.util.Util;

public class NewUpdateScreen extends WindowScreen {
    public NewUpdateScreen(Version latestVer) {
        super("New Update", true);

        add(new WLabel("New version of Meteor has been released.")); row();

        add(new WHorizontalSeparator()).fillX().expandX(); row();

        WTable versionsTable = add(new WTable()).getWidget(); row();
        versionsTable.add(new WLabel("Your version:"));
        versionsTable.add(new WLabel(Config.INSTANCE.version.toString())); versionsTable.row();
        versionsTable.add(new WLabel("Latest version:"));
        versionsTable.add(new WLabel(latestVer.toString()));

        add(new WHorizontalSeparator()).fillX().expandX(); row();

        add(new WButton("Download " + latestVer)).fillX().expandX().getWidget().action = button -> Util.getOperatingSystem().open("https://meteorclient.com/");
        add(new WButton("OK")).fillX().expandX().getWidget().action = button -> onClose();
    }
}
