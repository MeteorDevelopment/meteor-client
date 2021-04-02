/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import meteordevelopment.orbit.EventHandler;
import minegame159.meteorclient.events.packets.PacketEvent;
import minegame159.meteorclient.gui.GuiTheme;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.gui.widgets.containers.WHorizontalList;
import minegame159.meteorclient.gui.widgets.pressable.WButton;
import minegame159.meteorclient.modules.Categories;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.utils.player.ChatUtils;
import minegame159.meteorclient.waypoints.Waypoint;
import minegame159.meteorclient.waypoints.Waypoints;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.text.BaseText;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.Vec3d;

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

    private final Setting<Boolean> showTimestamp = sgGeneral.add(new BoolSetting.Builder()
            .name("show-timestamp")
            .description("Show timestamp in chat.")
            .defaultValue(true)
            .build()
    );
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private final Map<String, Double> deathPos = new HashMap<>();
    private Waypoint waypoint;

    private Vec3d dmgPos;

    private String labelText = "No latest death";

    public DeathPosition() {
        super(Categories.Player, "death-position", "Sends you the coordinates to your latest death.");
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof HealthUpdateS2CPacket) {
            HealthUpdateS2CPacket packet = (HealthUpdateS2CPacket) event.packet;

            if (packet.getHealth() <= 0) onDeath();
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WHorizontalList list = theme.horizontalList();

        WLabel label = list.add(theme.label(labelText)).expandCellX().widget();

        WButton path = list.add(theme.button("Path")).widget();
        path.action = this::path;

        WButton clear = list.add(theme.button("Clear")).widget();
        clear.action = () -> {
            Waypoints.get().remove(waypoint);
            labelText = "No latest death";

            label.set(labelText);
        };

        return list;
    }

    private void onDeath() {
        dmgPos = mc.player.getPos();
        deathPos.put("x", dmgPos.x);
        deathPos.put("z", dmgPos.z);
        labelText = String.format("Latest death: %.1f, %.1f, %.1f", dmgPos.x, dmgPos.y, dmgPos.z);

        String time = dateFormat.format(new Date());
        //ChatUtils.moduleInfo(this, "Died at (highlight)%.0f(default), (highlight)%.0f(default), (highlight)%.0f (default)on (highlight)%s(default).", damagedplayerX, damagedplayerY, damagedplayerZ, time);
        BaseText msg = new LiteralText("Died at ");
        msg.append(ChatUtils.formatCoords(dmgPos));
        msg.append(showTimestamp.get() ? String.format(" on %s.", time) : ".");
        ChatUtils.moduleInfo(this,msg);

        // Create waypoint
        if (createWaypoint.get()) {
            waypoint = new Waypoint();
            waypoint.name = "Death " + time;

            waypoint.x = (int) dmgPos.x;
            waypoint.y = (int) dmgPos.y + 2;
            waypoint.z = (int) dmgPos.z;
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

    private void path() {
        if (deathPos.isEmpty() && mc.player != null) {
            ChatUtils.moduleWarning(this,"No latest death found.");
        }
        else {
            if (mc.world != null) {
                double x = dmgPos.x, z = dmgPos.z;
                if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing()) {
                    BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
                }

                BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ((int) x, (int) z));
            }
        }
    }
}
