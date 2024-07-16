/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.systems.modules.render;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.EditSystemScreen;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import static meteordevelopment.meteorclient.utils.player.ChatUtils.formatCoords;

public class WaypointsModule extends Module {
    private static final Color GRAY = new Color(200, 200, 200);
    private static final Color TEXT = new Color(255, 255, 255);

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

    private final Setting<Integer> waypointFadeDistance = sgGeneral.add(new IntSetting.Builder()
        .name("waypoint-fade-distance")
        .description("The distance to a waypoint at which it begins to start fading.")
        .defaultValue(20)
        .sliderRange(0, 100)
        .min(0)
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
    private void onRender2D(Render2DEvent event) {
        TextRenderer text = TextRenderer.get();
        Vector3d center = new Vector3d(mc.getWindow().getFramebufferWidth() / 2.0, mc.getWindow().getFramebufferHeight() / 2.0, 0);
        int textRenderDist = textRenderDistance.get();

        for (Waypoint waypoint : Waypoints.get()) {
            // Continue if this waypoint should not be rendered
            if (!waypoint.visible.get() || !Waypoints.checkDimension(waypoint)) continue;

            // Calculate distance
            BlockPos blockPos = waypoint.getPos();
            Vector3d pos = new Vector3d(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
            double dist = PlayerUtils.distanceToCamera(pos.x, pos.y, pos.z);

            // Continue if this waypoint should not be rendered
            if (dist > waypoint.maxVisible.get()) continue;
            if (!NametagUtils.to2D(pos, 1)) continue;

            // Calculate alpha and distance to center of the screen
            double distToCenter = pos.distance(center);
            double a = 1;

            if (dist < waypointFadeDistance.get()) {
                a = (dist - (waypointFadeDistance.get() / 2d)) / (waypointFadeDistance.get() / 2d);
                if (a < 0.01) continue;
            }

            // Render
            NametagUtils.scale = waypoint.scale.get() - 0.2;
            NametagUtils.begin(pos);

            // Render icon
            waypoint.renderIcon(-16, -16, a, 32);

            // Render text if cursor is close enough
            if (distToCenter <= textRenderDist) {
                // Setup text rendering
                int preTextA = TEXT.a;
                TEXT.a *= a;
                text.begin();

                // Render name
                text.render(waypoint.name.get(), -text.getWidth(waypoint.name.get()) / 2, -16 - text.getHeight(), TEXT, true);

                // Render distance
                String distText = String.format("%d blocks", (int) Math.round(dist));
                text.render(distText, -text.getWidth(distText) / 2, 16, TEXT, true);

                // End text rendering
                text.end();
                TEXT.a = preTextA;
            }

            NametagUtils.end();
        }
    }

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
                .pos(BlockPos.ofFloored(deathPos).up(2))
                .dimension(PlayerUtils.getDimension())
                .build();

            Waypoints.get().add(waypoint);
        }

        cleanDeathWPs(maxDeathPositions.get());
    }

    private void cleanDeathWPs(int max) {
        int oldWpC = 0;

        for (Iterator<Waypoint> it = Waypoints.get().iterator(); it.hasNext();) {
            Waypoint wp = it.next();

            if (wp.name.get().startsWith("Death ") && wp.icon.get().equals("skull")) {
                oldWpC++;

                if (oldWpC > max)
                    it.remove();
            }
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        if (!Utils.canUpdate()) return theme.label("You need to be in a world.");

        WTable table = theme.table();
        initTable(theme, table);
        return table;
    }

    private void initTable(GuiTheme theme, WTable table) {
        table.clear();

        for (Waypoint waypoint : Waypoints.get()) {
            boolean validDim = Waypoints.checkDimension(waypoint);

            table.add(new WIcon(waypoint));

            WLabel name = table.add(theme.label(waypoint.name.get())).expandCellX().widget();
            if (!validDim) name.color = GRAY;

            WCheckbox visible = table.add(theme.checkbox(waypoint.visible.get())).widget();
            visible.action = () -> {
                waypoint.visible.set(visible.checked);
                Waypoints.get().save();
            };

            WButton edit = table.add(theme.button(GuiRenderer.EDIT)).widget();
            edit.action = () -> mc.setScreen(new EditWaypointScreen(theme, waypoint, () -> initTable(theme, table)));

            // Goto
            if (validDim) {
                WButton gotoB = table.add(theme.button("Goto")).widget();
                gotoB.action = () -> {
                    if (PathManagers.get().isPathing())
                        PathManagers.get().stop();

                    PathManagers.get().moveTo(waypoint.getPos());
                };
            }

            WMinus remove = table.add(theme.minus()).widget();
            remove.action = () -> {
                Waypoints.get().remove(waypoint);
                initTable(theme, table);
            };

            table.row();
        }

        table.add(theme.horizontalSeparator()).expandX();
        table.row();

        WButton create = table.add(theme.button("Create")).expandX().widget();
        create.action = () -> mc.setScreen(new EditWaypointScreen(theme, null, () -> initTable(theme, table)));
    }

    private static class EditWaypointScreen extends EditSystemScreen<Waypoint> {
        public EditWaypointScreen(GuiTheme theme, Waypoint value, Runnable reload) {
            super(theme, value, reload);
        }

        @Override
        public Waypoint create() {
            return new Waypoint.Builder()
                .pos(MinecraftClient.getInstance().player.getBlockPos().up(2))
                .dimension(PlayerUtils.getDimension())
                .build();
        }

        @Override
        public boolean save() {
            if (value.name.get().isBlank()) return false;

            Waypoints.get().add(value);
            return true;
        }

        @Override
        public Settings getSettings() {
            return value.settings;
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
