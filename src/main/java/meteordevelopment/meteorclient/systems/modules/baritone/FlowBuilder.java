/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.baritone;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.baritoneflow.FlowsScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.baritoneflow.BaritoneFlows;
import meteordevelopment.meteorclient.systems.baritoneflow.Flow;
import meteordevelopment.meteorclient.systems.baritoneflow.FlowNode;
import meteordevelopment.meteorclient.systems.baritoneflow.FlowRunner;
import meteordevelopment.meteorclient.systems.baritoneflow.FlowTriggers;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

/**
 * Visual node-flow editor and runner: build, save and run flows (see {@link FlowsScreen}) that chain
 * Baritone tasks, Meteor module actions and client actions together. Flows can also be armed to
 * start themselves from a trigger node (e.g. a player appearing nearby) - this module stays
 * subscribed across disconnects/reconnects ({@code runInMainMenu}) so a flow like "player appears
 * -> leave -> wait -> reconnect" keeps running while logged out. For one-off tasks without a flow,
 * see the other modules in the Baritone category (Goto, Mine, Follow, Pause, Resume, Stop, Command).
 */
public class FlowBuilder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> runOnEnable = sgGeneral.add(new StringSetting.Builder()
        .name("run-on-enable")
        .description("Name of a saved flow to start automatically when this module is enabled. Leave empty to only use the editor.")
        .defaultValue("")
        .build()
    );

    private final FlowRunner runner = new FlowRunner();
    private final FlowTriggers triggers = new FlowTriggers(this::onTriggerFired);

    public FlowBuilder() {
        super(Categories.Baritone, "flow-builder", "Visual node-flow editor for chaining Baritone tasks and Meteor module actions.");

        // Keeps armed flows' triggers watched, and any running flow (e.g. Leave -> Wait -> Reconnect) progressing, across disconnects.
        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        String name = runOnEnable.get();
        if (name != null && !name.isBlank()) {
            Flow flow = BaritoneFlows.get().get(name);
            if (flow != null) runner.start(flow);
            else warning("No saved flow named '%s'.", name);
        }
    }

    @Override
    public void onDeactivate() {
        runner.stop();
        triggers.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        runner.tick();
        triggers.tick();
    }

    /** Starts a flow, enabling the module first so it gets ticked. Called from the editor / flow list. */
    public void runFlow(Flow flow) {
        if (!isActive()) toggle();
        runner.start(flow);
        info("Running flow '%s'.", flow.name);
    }

    // Only ever called from onTick(), which only runs while this module is already active.
    private void onTriggerFired(Flow flow, FlowNode node) {
        runner.startFrom(flow, node);
        info("Trigger '%s' fired in '%s'.", node.title(), flow.name);
    }

    public void stopAll() {
        runner.stop();
        if (BaritoneUtils.IS_AVAILABLE) PathManagers.get().stop();
    }

    public boolean isRunningFlow() {
        return runner.isRunning();
    }

    public Flow getRunningFlow() {
        return runner.getFlow();
    }

    public FlowNode getCurrentNode() {
        return runner.getCurrentNode();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WButton flows = theme.button("Flows & Editor");
        flows.action = () -> mc.setScreen(new FlowsScreen(theme));

        return flows;
    }
}
