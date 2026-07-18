/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.baritoneflow;

import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.pathing.IPathManager;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Executes a {@link Flow} one node at a time, driving Baritone through {@link PathManagers}. Ticked
 * every game tick by the owning module. Each node either finishes instantly, waits for Baritone to
 * stop pathing, or waits for a fixed duration before moving on to the next node in the flow's order.
 */
public class FlowRunner {
    private static final long PATHING_TIMEOUT_MS = 10 * 60 * 1000; // safety net so a stuck path can't hang the flow forever

    private Flow flow;
    private List<FlowNode> order;
    private int index;
    private boolean running;

    private State state;
    private long nodeStartTime;
    private int startupTicks;

    private enum State {
        Begin,
        WaitPathing,
        WaitTime,
        Instant
    }

    public boolean isRunning() {
        return running;
    }

    public Flow getFlow() {
        return flow;
    }

    public FlowNode getCurrentNode() {
        return (running && order != null && index >= 0 && index < order.size()) ? order.get(index) : null;
    }

    public void start(Flow flow) {
        startFrom(flow, flow.getStart());
    }

    /** Starts execution from an arbitrary node, e.g. a trigger node whose condition just fired. */
    public void startFrom(Flow flow, FlowNode entry) {
        stop();

        this.flow = flow;
        this.order = flow.computeOrder(entry);
        this.index = 0;
        this.running = !order.isEmpty();
        this.state = State.Begin;
    }

    public void stop() {
        if (running && BaritoneUtils.IS_AVAILABLE) PathManagers.get().stop();

        running = false;
        flow = null;
        order = null;
        index = 0;
    }

    public void tick() {
        if (!running || order == null) return;
        if (index >= order.size()) {
            finish();
            return;
        }

        FlowNode node = order.get(index);
        IPathManager pm = PathManagers.get();

        switch (state) {
            case Begin -> begin(node, pm);
            case Instant -> advance();
            case WaitTime -> {
                if (System.currentTimeMillis() - nodeStartTime >= node.seconds.get() * 1000) advance();
            }
            case WaitPathing -> {
                if (startupTicks > 0) startupTicks--; // give Baritone a moment to actually start pathing
                else if (!pm.isPathing() || System.currentTimeMillis() - nodeStartTime > PATHING_TIMEOUT_MS) advance();
            }
        }
    }

    private void begin(FlowNode node, IPathManager pm) {
        switch (node.type.get()) {
            // Start/OnPlayerNearAppear are entry points only - reaching them just moves on to their children.
            case Start, OnPlayerNearAppear, Pause, Resume -> {
                if (node.type.get() == FlowNodeType.Pause && BaritoneUtils.IS_AVAILABLE) pm.pause();
                if (node.type.get() == FlowNodeType.Resume && BaritoneUtils.IS_AVAILABLE) pm.resume();
                state = State.Instant;
            }
            case Stop -> {
                pm.stop();
                finish();
            }
            case Goto -> {
                BlockPos pos = node.target.get();
                pm.moveTo(pos, node.ignoreY.get());
                beginPathing();
            }
            case Mine -> {
                List<Block> blocks = node.blocks.get();
                if (blocks.isEmpty()) {
                    state = State.Instant;
                } else {
                    pm.mine(blocks.toArray(new Block[0]));
                    beginPathing();
                }
            }
            case Follow -> {
                pm.follow(followPredicate(node));
                beginWait(); // follow is continuous, so keep it running for the node's duration then move on
            }
            case Wait -> beginWait();
            case Command -> {
                BaritoneUtils.runCommand(node.command.get());
                beginPathing(); // most commands (goto/mine/farm/build) put Baritone into a pathing state
            }
            case Module -> {
                executeModuleAction(node);
                state = State.Instant;
            }
            case Leave -> {
                leave();
                state = State.Instant;
            }
            case Reconnect -> {
                reconnect();
                state = State.Instant;
            }
        }
    }

    private void executeModuleAction(FlowNode node) {
        List<Module> modules = node.targetModule.get();
        if (modules.isEmpty()) return;

        Module module = modules.getFirst();

        switch (node.moduleAction.get()) {
            case Enable -> module.enable();
            case Disable -> module.disable();
            case Toggle -> module.toggle();
            case SetSetting -> {
                Setting<?> setting = module.settings.get(node.moduleSettingName.get());
                if (setting != null) setting.parse(node.moduleSettingValue.get());
            }
        }
    }

    private void leave() {
        if (mc.player == null || mc.player.connection == null) return;

        mc.player.connection.handleDisconnect(new ClientboundDisconnectPacket(Component.literal("Baritone Flow: Leave node")));
    }

    private void reconnect() {
        AutoReconnect autoReconnect = Modules.get().get(AutoReconnect.class);
        if (autoReconnect == null || autoReconnect.lastServerConnection == null) return;

        var lastServer = autoReconnect.lastServerConnection;
        ConnectScreen.startConnecting(new TitleScreen(), mc, lastServer.left(), lastServer.right(), false, null);
    }

    private void beginPathing() {
        startupTicks = 5;
        nodeStartTime = System.currentTimeMillis();
        state = State.WaitPathing;
    }

    private void beginWait() {
        nodeStartTime = System.currentTimeMillis();
        state = State.WaitTime;
    }

    private void advance() {
        index++;
        state = State.Begin;
        if (index >= order.size()) finish();
    }

    private void finish() {
        running = false;
        flow = null;
        order = null;
        index = 0;
    }

    private Predicate<Entity> followPredicate(FlowNode node) {
        return switch (node.followTarget.get()) {
            case NearestPlayer -> entity -> entity instanceof Player && entity != mc.player;
            case NearestEntity -> entity -> entity != mc.player && entity.isAlive();
            case PlayerByName -> {
                String name = node.followName.get();
                yield entity -> entity instanceof Player && entity.getName().getString().equalsIgnoreCase(name);
            }
        };
    }
}
