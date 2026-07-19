/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.baritoneflow;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.baritoneflow.DisconnectLog;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Lists every disconnect reason recorded by {@link DisconnectLog}, newest first, with a way to clear the history. */
public class DisconnectLogScreen extends WindowScreen {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    public DisconnectLogScreen(GuiTheme theme) {
        super(theme, "Disconnect Log");
    }

    @Override
    public void initWidgets() {
        DisconnectLog log = DisconnectLog.get();

        WHorizontalList top = add(theme.horizontalList()).expandX().widget();
        top.add(theme.label(log.getAll().size() + " recorded disconnect(s).")).expandCellX().widget();

        WButton clear = top.add(theme.button("Clear")).widget();
        clear.action = () -> {
            log.clear();
            reload();
        };

        add(theme.horizontalSeparator()).expandX();

        if (log.getAll().isEmpty()) {
            add(theme.label("No disconnects recorded yet."));
            return;
        }

        for (DisconnectLog.Entry entry : log.getAll()) {
            WHorizontalList row = add(theme.horizontalList()).expandX().widget();

            row.add(theme.label(TIME_FORMAT.format(Instant.ofEpochMilli(entry.time())))).widget();

            String reason = entry.reason().isBlank() ? "(unknown)" : entry.reason().replace("\n", " ");
            row.add(theme.label(reason)).expandCellX().widget();
        }
    }
}
