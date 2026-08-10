/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.baritoneflow;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.baritoneflow.Flow;
import meteordevelopment.meteorclient.systems.baritoneflow.FlowNode;
import meteordevelopment.meteorclient.systems.baritoneflow.FlowNodeType;

/**
 * Picker shown when right-clicking empty canvas in the {@link FlowEditorScreen}. Adds a node of the
 * chosen type at the click position.
 */
public class AddFlowNodeScreen extends WindowScreen {
    private final Flow flow;
    private final int x, y;

    public AddFlowNodeScreen(GuiTheme theme, Flow flow, int x, int y) {
        super(theme, "Add Node");

        this.flow = flow;
        this.x = x;
        this.y = y;
    }

    @Override
    public void initWidgets() {
        WSection triggers = add(theme.section("Triggers")).expandX().widget();
        WSection baritone = add(theme.section("Baritone Tasks")).expandX().widget();
        WSection modules = add(theme.section("Meteor Modules")).expandX().widget();
        WSection client = add(theme.section("Client Actions")).expandX().widget();

        for (FlowNodeType type : FlowNodeType.values()) {
            if (type == FlowNodeType.Start) continue; // only one Start node per flow

            WSection section = switch (type.category()) {
                case Trigger -> triggers;
                case Module -> modules;
                case Client -> client;
                case Baritone -> baritone;
            };

            WButton button = section.add(theme.button(type.label())).expandX().widget();
            button.action = () -> {
                FlowNode node = flow.addNode(x, y);
                node.type.set(type);
                onClose();
            };
        }
    }
}
