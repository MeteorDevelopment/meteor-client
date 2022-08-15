/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalGetToBlock;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ListIterator;

import static meteordevelopment.meteorclient.utils.player.ChatUtils.formatCoords;

public class WaypointsModule extends Module {
    private static final Color GRAY = new Color(200, 200, 200);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDeathPosition = settings.createGroup("Death Position");

    public final Setting<Integer> textRenderDistance = sgGeneral.add(new IntSetting.Builder()
        .name("text-render-distance")
        .description("Maximum distance from the center of the screen at which text will be rendered.")
        .defaultValue(100)
        .min(0)
        .sliderMax(200)
        .build()
    );

    private final Setting<Integer> maxDeathPositions = sgDeathPosition.add(new IntSetting.Builder()
        .name("max-death-positions")
        .description("The amount of death positions to save, 0 to disable")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .onChanged(this::cleanDeathWPs)
        .build()
    );

    private final Setting<Boolean> dpChat = sgDeathPosition.add(new BoolSetting.Builder()
        .name("chat")
        .description("Send a chat message with your position once you die")
        .defaultValue(false)
        .build()
    );

    public WaypointsModule() {
        super(Categories.Render, "waypoints", "Allows you to create waypoints.");
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (!(event.screen instanceof DeathScreen)) return;

        if (!event.isCancelled()) addDeath(mc.player.getPos());
    }

    public void addDeath(Vec3d deathPos) {
        String time = dateFormat.format(new Date());
        if (dpChat.get()) {
            MutableText text = Text.literal("Died at ");
            text.append(formatCoords(deathPos));
            text.append(String.format(" on %s.", time));
            info(text);
        }

        // Create waypoint
        if (maxDeathPositions.get() > 0) {
            Waypoint waypoint = new Waypoint.Builder()
                .name("Death " + time)
                .icon("skull")
                .pos(new BlockPos(deathPos).up(2))
                .dimension(PlayerUtils.getDimension())
                .build();

            Waypoints.get().add(waypoint);
        }

        cleanDeathWPs(maxDeathPositions.get());
    }

    private void cleanDeathWPs(int max) {
        int oldWpC = 0;

        ListIterator<Waypoint> wps = Waypoints.get().iteratorReverse();
        while (wps.hasPrevious()) {
            Waypoint wp = wps.previous();
            if (wp.name.get().startsWith("Death ") && "skull".equals(wp.icon.get())) {
                oldWpC++;
                if (oldWpC > max)
                    Waypoints.get().remove(wp);
            }
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        if (!Utils.canUpdate()) return theme.label("You need to be in a world.");

        WTable table = theme.table();
        fillTable(theme, table);
        return table;
    }

    private void fillTable(GuiTheme theme, WTable table) {
        // Create
        WButton create = table.add(theme.button("Create")).expandX().widget();
        create.action = () -> mc.setScreen(new EditWaypointScreen(theme, null, () -> {
            table.clear();
            fillTable(theme, table);
        }));
        table.row();

        // Waypoints
        for (Waypoint waypoint : Waypoints.get()) {
            // Icon
            table.add(new WIcon(waypoint));

            // Name
            WLabel name = table.add(theme.label(waypoint.name.get())).expandCellX().widget();
            if (!Waypoints.checkDimension(waypoint)) name.color = GRAY;

            // Visible
            WCheckbox visible = table.add(theme.checkbox(waypoint.visible.get())).widget();
            visible.action = () -> {
                waypoint.visible.set(visible.checked);
                Waypoints.get().save();
            };

            // Edit
            WButton edit = table.add(theme.button(GuiRenderer.EDIT)).widget();
            edit.action = () -> mc.setScreen(new EditWaypointScreen(theme, waypoint, null));

            // Remove
            WMinus remove = table.add(theme.minus()).widget();
            remove.action = () -> {
                Waypoints.get().remove(waypoint);

                table.clear();
                fillTable(theme, table);
            };

            // Goto
            if (waypoint.dimension.get().equals(PlayerUtils.getDimension())) {
                WButton gotoB = table.add(theme.button("Goto")).widget();
                gotoB.action = () -> {
                    if (mc.player == null || mc.world == null) return;
                    IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
                    if (baritone.getPathingBehavior().isPathing()) baritone.getPathingBehavior().cancelEverything();
                    baritone.getCustomGoalProcess().setGoalAndPath(new GoalGetToBlock(waypoint.getPos()));
                };
            }

            table.row();
        }
    }

    private class EditWaypointScreen extends WindowScreen {
        private WContainer settingsContainer;
        private final Waypoint waypoint;
        private final boolean newWaypoint;
        private final Runnable action;

        public EditWaypointScreen(GuiTheme theme, Waypoint waypoint, Runnable action) {
            super(theme, waypoint == null ? "New Waypoint" : "Edit Waypoint");

            this.newWaypoint = waypoint == null;
            this.action = action;

            if (newWaypoint) {
                this.waypoint = new Waypoint.Builder()
                    .pos(mc.player.getBlockPos().up(2))
                    .dimension(PlayerUtils.getDimension())
                    .build();
            }
            else {
                this.waypoint = waypoint;
            }
        }

        @Override
        public void initWidgets() {
            settingsContainer = add(theme.verticalList()).expandX().widget();
            settingsContainer.add(theme.settings(waypoint.settings)).expandX();

            add(theme.horizontalSeparator()).expandX();

            // Save
            WButton save = add(theme.button("Save")).expandX().widget();
            save.action = () -> {
                if (newWaypoint) Waypoints.get().add(waypoint);
                else Waypoints.get().save();

                close();
            };

            enterAction = save.action;
        }

        @Override
        public void tick() {
            waypoint.settings.tick(settingsContainer, theme);
        }

        @Override
        protected void onClosed() {
            if (action != null) action.run();
        }
    }

    private static class WIcon extends WWidget {
        private final Waypoint waypoint;

        public WIcon(Waypoint waypoint) {
            this.waypoint = waypoint;
        }

        @Override
        protected void onCalculateSize() {
            double s = theme.scale(32);

            width = s;
            height = s;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderer.post(() -> waypoint.renderIcon(x, y, 1, width));
        }
    }
}
