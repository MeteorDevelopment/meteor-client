/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.baritoneflow;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.baritoneflow.BaritoneFlows;
import meteordevelopment.meteorclient.systems.baritoneflow.Flow;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.BaritoneFlow;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Lists all saved {@link Flow}s and lets you create, open (in the editor), run and delete them.
 */
public class FlowsScreen extends WindowScreen {
    private WTextBox nameBox;

    public FlowsScreen(GuiTheme theme) {
        super(theme, "Baritone Flows");
    }

    @Override
    public void initWidgets() {
        // Create new flow
        WHorizontalList create = add(theme.horizontalList()).expandX().widget();
        nameBox = create.add(theme.textBox("")).expandX().widget();
        nameBox.setFocused(true);

        WButton createBtn = create.add(theme.button("Create")).widget();
        createBtn.action = this::createFlow;
        enterAction = createBtn.action;

        add(theme.horizontalSeparator()).expandX();

        BaritoneFlows flows = BaritoneFlows.get();
        if (flows.isEmpty()) {
            add(theme.label("No saved flows yet. Type a name above and click Create."));
            return;
        }

        for (Flow flow : flows) {
            WHorizontalList row = add(theme.horizontalList()).expandX().widget();

            row.add(theme.label(flow.name)).expandCellX().widget();

            WButton edit = row.add(theme.button("Edit")).widget();
            edit.action = () -> mc.setScreen(new FlowEditorScreen(theme, flow));

            WButton run = row.add(theme.button("Run")).widget();
            run.action = () -> {
                BaritoneFlow module = Modules.get().get(BaritoneFlow.class);
                if (module != null) module.runFlow(flow);
                onClose();
            };

            WButton delete = row.add(theme.button("Delete")).widget();
            delete.action = () -> {
                flows.remove(flow);
                reload();
            };
        }
    }

    private void createFlow() {
        String name = nameBox.get().trim();
        if (name.isEmpty() || BaritoneFlows.get().get(name) != null) return;

        Flow flow = new Flow(name);
        BaritoneFlows.get().add(flow);
        mc.setScreen(new FlowEditorScreen(theme, flow));
    }
}
