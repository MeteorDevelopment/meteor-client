/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.world;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.baritoneflow.FlowsScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.baritoneflow.BaritoneFlows;
import meteordevelopment.meteorclient.systems.baritoneflow.Flow;
import meteordevelopment.meteorclient.systems.baritoneflow.FlowNode;
import meteordevelopment.meteorclient.systems.baritoneflow.FlowRunner;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Friendly front-end for Baritone: fire off common tasks with one click, and build, save and run
 * visual node "flows" (see {@link FlowsScreen}) that chain multiple Baritone tasks together.
 */
public class BaritoneFlow extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgQuick = settings.createGroup("Quick Task");

    private final Setting<String> runOnEnable = sgGeneral.add(new StringSetting.Builder()
        .name("run-on-enable")
        .description("Name of a saved flow to start automatically when this module is enabled. Leave empty to only use quick tasks / the editor.")
        .defaultValue("")
        .build()
    );

    private final Setting<BlockPos> gotoPos = sgQuick.add(new BlockPosSetting.Builder()
        .name("goto-target")
        .description("Destination for the \"Go To Target\" button.")
        .build()
    );

    private final Setting<List<Block>> mineBlocks = sgQuick.add(new BlockListSetting.Builder()
        .name("mine-blocks")
        .description("Blocks for the \"Mine\" button.")
        .build()
    );

    private final FlowRunner runner = new FlowRunner();

    public BaritoneFlow() {
        super(Categories.World, "baritone-flow", "Simplifies Baritone with one-click tasks and a visual node-flow editor.");
    }

    @Override
    public void onActivate() {
        if (!checkBaritone()) return;

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
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        runner.tick();
    }

    /** Starts a flow, enabling the module first so it gets ticked. Called from the editor / flow list. */
    public void runFlow(Flow flow) {
        if (!checkBaritone()) return;

        if (!isActive()) toggle();
        runner.start(flow);
        info("Running flow '%s'.", flow.name);
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
        WVerticalList list = theme.verticalList();

        WButton flows = list.add(theme.button("Flows & Editor")).expandX().widget();
        flows.action = () -> mc.setScreen(new FlowsScreen(theme));

        WHorizontalList quick = list.add(theme.horizontalList()).expandX().widget();

        WButton go = quick.add(theme.button("Go To Target")).expandX().widget();
        go.action = this::runGoto;

        WButton mine = quick.add(theme.button("Mine")).expandX().widget();
        mine.action = this::runMine;

        WButton stop = quick.add(theme.button("Stop")).expandX().widget();
        stop.action = this::stopAll;

        return list;
    }

    private void runGoto() {
        if (!checkBaritone()) return;
        PathManagers.get().moveTo(gotoPos.get(), false);
    }

    private void runMine() {
        if (!checkBaritone()) return;

        if (mineBlocks.get().isEmpty()) {
            warning("No blocks selected in the Quick Task group.");
            return;
        }

        PathManagers.get().mine(mineBlocks.get().toArray(new Block[0]));
    }

    private boolean checkBaritone() {
        if (!BaritoneUtils.IS_AVAILABLE) {
            error("Baritone is not installed.");
            return false;
        }
        return true;
    }
}
