/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import baritone.api.BaritoneAPI;
import net.minecraft.util.math.Vec3d;
import baritone.api.pathing.goals.GoalXZ;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.entity.TookDamageEvent;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.waypoints.Waypoint;
import minegame159.meteorclient.waypoints.Waypoints;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DeathPosition extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> createWaypoint = sgGeneral.add(new BoolSetting.Builder()
            .name("create-waypoint")
            .description("Creates a waypoint when you die.")
            .defaultValue(true)
            .build()
    );

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private final WLabel label = new WLabel("No latest death found.");

    private final Map<String, Double> deathPos = new HashMap<>();
    private Waypoint waypoint;

    private double damagedplayerX;
    private double damagedplayerY;
    private double damagedplayerZ;

    public DeathPosition() {
        super(Categories.Player, "death-position", "Sends you the coordinates to your latest death.");
    }

    @EventHandler
    private void onTookDamage(TookDamageEvent event) {
        if (mc.player == null) return;
        
        if (event.entity.getUuid() != null && event.entity.getUuid().equals(mc.player.getUuid()) && event.entity.getHealth() >= 0) {
            damagedplayerX = mc.player.getX();
            damagedplayerY = mc.player.getY();
            damagedplayerZ = mc.player.getZ(); }

        if (event.entity.getUuid() != null && event.entity.getUuid().equals(mc.player.getUuid()) && event.entity.getHealth() <= 0) {
            deathPos.put("x", damagedplayerX);
            deathPos.put("z", damagedplayerZ);
            label.setText(String.format("Latest death: %.1f, %.1f, %.1f", damagedplayerX, damagedplayerY, damagedplayerZ));

            String time = dateFormat.format(new Date());
            //ChatUtils.moduleInfo(this, "Died at (highlight)%.0f(default), (highlight)%.0f(default), (highlight)%.0f (default)on (highlight)%s(default).", damagedplayerX, damagedplayerY, damagedplayerZ, time);
            BaseText msg = new LiteralText("Died at ");
            Vec3d damagedcords = new Vec3d(damagedplayerX, damagedplayerY, damagedplayerZ);
            msg.append(ChatUtils.formatCoords(damagedcords));
            msg.append(".");
            ChatUtils.moduleInfo(this,msg);

            // Create waypoint
            if (createWaypoint.get()) {
                waypoint = new Waypoint();
                waypoint.name = "Death " + time;

                waypoint.x = (int) damagedplayerX;
                waypoint.y = (int) damagedplayerY + 2;
                waypoint.z = (int) damagedplayerZ;
                waypoint.maxVisibleDistance = Integer.MAX_VALUE;
                waypoint.actualDimension = Utils.getDimension();

                switch (Utils.getDimension()) {
                    case Overworld:
                        waypoint.overworld = true;
                        break;
                    case Nether:
                        waypoint.nether = true;
                        break;
                    case End:
                        waypoint.end = true;
                        break;
                }

                Waypoints.get().add(waypoint);
            }
        }
    }

    @Override
    public WWidget getWidget() {
        WTable table = new WTable();
        table.add(label);
        WButton path = new WButton("Path");
        table.add(path);
        path.action = this::path;
        WButton clear = new WButton("Clear");
        table.add(clear);
        clear.action = this::clear;
        return table;
    }

    private void path() {
        if (deathPos.isEmpty() && mc.player != null) {
            ChatUtils.moduleWarning(this,"No latest death found.");
        } else {
            if (mc.world != null) {
                double x = damagedplayerX, z = damagedplayerZ;
                if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
                    BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ((int) x, (int) z));
            }
        }
    }

    private void clear() {
        Waypoints.get().remove(waypoint);
        label.setText("No latest death.");
    }
}
