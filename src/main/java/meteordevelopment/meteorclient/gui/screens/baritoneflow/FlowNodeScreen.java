/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.baritoneflow;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.baritoneflow.BaritoneFlows;
import meteordevelopment.meteorclient.systems.baritoneflow.Flow;
import meteordevelopment.meteorclient.systems.baritoneflow.FlowNode;
import meteordevelopment.meteorclient.systems.baritoneflow.FlowNodeType;

/**
 * Edits a single {@link FlowNode}'s parameters (rendered from its {@link FlowNode#settings}) and
 * lets you delete it. Changes are saved when the screen closes.
 */
public class FlowNodeScreen extends WindowScreen {
    private final Flow flow;
    private final FlowNode node;
    private WContainer settingsContainer;

    public FlowNodeScreen(GuiTheme theme, Flow flow, FlowNode node) {
        super(theme, "Edit Node");

        this.flow = flow;
        this.node = node;
    }

    @Override
    public void initWidgets() {
        settingsContainer = add(theme.verticalList()).expandX().widget();
        settingsContainer.add(theme.settings(node.settings)).expandX();

        if (node.type.get() != FlowNodeType.Start) {
            add(theme.horizontalSeparator()).expandX();

            WButton delete = add(theme.button("Delete Node")).expandX().widget();
            delete.action = () -> {
                flow.removeNode(node);
                onClose();
            };
        }
    }

    @Override
    public void tick() {
        node.settings.tick(settingsContainer, theme);
    }

    @Override
    protected void onClosed() {
        BaritoneFlows.get().save();
    }
}
