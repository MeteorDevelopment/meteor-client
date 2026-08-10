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
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * Runs a {@link Flow} as a queue of tasks, one at a time, driving Baritone through
 * {@link PathManagers}. Nodes are pulled off the front of the queue; each one either finishes
 * instantly, waits for Baritone to stop pathing, or waits a fixed duration before the next is
 * pulled. This makes big sequential flows (goto -> mine -> leave) run reliably for a long time.
 *
 * <p>Trigger nodes fire via {@link #interrupt(Flow, FlowNode)}: their branch is pushed to the front
 * of the queue and the currently running, resumable task (goto/mine/follow/command) is stopped and
 * re-queued so it resumes once the handler finishes.</p>
 */
public class FlowRunner {
    private static final double PATH_FACTOR = 1.3; // real Baritone paths aren't straight lines, so pad the estimate

    private final Deque<FlowNode> queue = new ArrayDeque<>();

    private Flow flow;
    private FlowNode current;
    private boolean running;
    private boolean paused;

    private String lastDisconnectReason = "";

    private State state;
    private long nodeStartTime;
    private int startupTicks;

    // ETA tracking for the current Goto target.
    private BlockPos currentTarget;
    private double lastX, lastZ;
    private boolean hasLastPos;
    private double smoothedSpeed; // horizontal blocks per second, smoothed - captures the player's real pace/gear

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
        return running ? current : null;
    }

    /** Runs the flow from its Start node. */
    public void start(Flow flow) {
        stop();

        this.flow = flow;
        queue.addAll(flow.computeOrder());
        running = !queue.isEmpty();
    }

    /**
     * Injects a trigger node's branch at the front of the queue as an interrupt. The currently
     * running resumable task is stopped and re-queued behind the handler so it resumes afterwards.
     */
    public void interrupt(Flow flow, FlowNode triggerNode) {
        List<FlowNode> branch = flow.computeOrder(triggerNode);
        if (branch.isEmpty()) return;

        this.flow = flow;

        // Stop and re-queue the current resumable task so it resumes after the handler runs.
        if (current != null && isResumable(current)) {
            if (BaritoneUtils.IS_AVAILABLE) PathManagers.get().stop();
            queue.addFirst(current);
        }

        // Push the handler branch to the front (reverse order so its first node runs first).
        for (int i = branch.size() - 1; i >= 0; i--) queue.addFirst(branch.get(i));

        current = null; // force the next tick to pull the handler
        running = true;
    }

    public void stop() {
        if (running && BaritoneUtils.IS_AVAILABLE) PathManagers.get().stop();

        running = false;
        paused = false;
        flow = null;
        current = null;
        currentTarget = null;
        queue.clear();
    }

    /**
     * Pauses the whole flow - not just Baritone's current path - so {@link #tick()} stops pulling
     * the next queued node while paused. Without this, pausing Baritone mid-Goto makes
     * {@code pm.isPathing()} report false, which the queue would otherwise read as "task finished"
     * and immediately advance to whatever comes next (e.g. a Leave node disconnecting the player).
     */
    public void pause() {
        if (!running) return;

        paused = true;
        if (BaritoneUtils.IS_AVAILABLE) PathManagers.get().pause();
    }

    public void resume() {
        if (!running) return;

        paused = false;
        if (BaritoneUtils.IS_AVAILABLE) PathManagers.get().resume();
    }

    public boolean isPaused() {
        return paused;
    }

    public String getLastDisconnectReason() {
        return lastDisconnectReason;
    }

    public void setLastDisconnectReason(String reason) {
        lastDisconnectReason = reason == null ? "" : reason;
    }

    /**
     * Estimated time of arrival for the current Goto target, or null when there's nothing to path to.
     * Uses the straight-line distance, a smoothed measurement of the player's real speed (which
     * captures their gear/experience), and a path-inefficiency factor.
     */
    public String getEtaText() {
        if (!running || currentTarget == null || mc.player == null) return null;

        double dx = currentTarget.getX() + 0.5 - mc.player.getX();
        double dz = currentTarget.getZ() + 0.5 - mc.player.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < 2) return null; // basically arrived

        if (smoothedSpeed < 0.5) return "ETA calculating...  " + Math.round(distance) + "m";

        double etaSeconds = distance * PATH_FACTOR / smoothedSpeed;
        return "ETA " + formatTime(etaSeconds) + "  (" + Math.round(distance) + "m @ " + String.format("%.1f", smoothedSpeed) + " b/s)";
    }

    private void updateSpeed() {
        if (mc.player == null) {
            hasLastPos = false;
            return;
        }

        double x = mc.player.getX();
        double z = mc.player.getZ();

        if (hasLastPos) {
            double dx = x - lastX;
            double dz = z - lastZ;
            double instant = Math.sqrt(dx * dx + dz * dz) * 20.0; // blocks/tick -> blocks/second
            smoothedSpeed = smoothedSpeed * 0.9 + instant * 0.1;
        }

        lastX = x;
        lastZ = z;
        hasLastPos = true;
    }

    private static String formatTime(double seconds) {
        long s = Math.round(seconds);
        if (s >= 3600) return (s / 3600) + "h " + ((s % 3600) / 60) + "m";
        if (s >= 60) return (s / 60) + "m " + (s % 60) + "s";
        return s + "s";
    }

    public void tick() {
        if (!running) return;

        updateSpeed();
        if (paused) return;

        if (current == null) {
            current = queue.pollFirst();
            if (current == null) {
                finish();
                return;
            }
            state = State.Begin;
        }

        IPathManager pm = PathManagers.get();

        switch (state) {
            case Begin -> begin(current, pm);
            case Instant -> complete();
            case WaitTime -> {
                if (System.currentTimeMillis() - nodeStartTime >= current.seconds.get() * 1000) complete();
            }
            case WaitPathing -> {
                double max = current.maxDuration.get();
                boolean timedOut = max > 0 && System.currentTimeMillis() - nodeStartTime > max * 1000;

                if (startupTicks > 0) startupTicks--; // give Baritone a moment to actually start pathing
                else if (!pm.isPathing() || timedOut) complete();
            }
        }
    }

    private void begin(FlowNode node, IPathManager pm) {
        // Baritone/world actions do nothing while disconnected - skip them so the queue keeps
        // draining (important for Leave -> Wait -> Reconnect and mid-flow reconnects).
        boolean offline = mc.player == null;

        currentTarget = null; // only the currently running Goto has an ETA target

        switch (node.type.get()) {
            // Entry-point / marker nodes just pass through to their children.
            case Start, OnPlayerNearAppear, OnHunger, OnHostileMobsNear, OnLowHealth, OnInventoryFull -> state = State.Instant;

            case Pause -> {
                if (BaritoneUtils.IS_AVAILABLE) pm.pause();
                state = State.Instant;
            }
            case Resume -> {
                if (BaritoneUtils.IS_AVAILABLE) pm.resume();
                state = State.Instant;
            }
            case Stop -> {
                pm.stop();
                finish();
            }
            case Goto -> {
                if (offline) state = State.Instant;
                else {
                    currentTarget = node.target.get();
                    pm.moveTo(node.target.get(), node.ignoreY.get());
                    beginPathing();
                }
            }
            case Mine -> {
                if (offline || node.blocks.get().isEmpty()) state = State.Instant;
                else {
                    pm.mine(node.blocks.get().toArray(new Block[0]));
                    beginPathing();
                }
            }
            case Follow -> {
                if (offline) state = State.Instant;
                else {
                    pm.follow(followPredicate(node));
                    beginWait(); // follow is continuous, so keep it running for the node's duration then move on
                }
            }
            case Wait -> beginWait();
            case Command -> {
                if (offline) state = State.Instant;
                else {
                    BaritoneUtils.runCommand(node.command.get());
                    beginPathing(); // most commands (goto/mine/farm/build) put Baritone into a pathing state
                }
            }
            case Module -> {
                executeModuleAction(node);
                state = State.Instant;
            }
            case Notify -> {
                runNotify(node);
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

    private boolean isResumable(FlowNode node) {
        return switch (node.type.get()) {
            case Goto, Mine, Follow, Command -> true;
            default -> false;
        };
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

    private void runNotify(FlowNode node) {
        String message = node.notifyMessage.get();
        if (message == null || message.isBlank()) message = "Flow notification";
        message = message.replace("%reason%", lastDisconnectReason);

        ChatUtils.info("[Baritone Flow] %s", message);
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

    /** Marks the current node done; the next tick pulls the next node from the queue. */
    private void complete() {
        current = null;
    }

    private void finish() {
        running = false;
        paused = false;
        flow = null;
        current = null;
        currentTarget = null;
        queue.clear();
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
